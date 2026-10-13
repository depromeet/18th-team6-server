package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.category.repository.CategoryIconRepository
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.global.log.analytics.entity.AnalyticsEventEntity
import depromeet.hotsix.obrit.global.log.analytics.repository.AnalyticsEventRepository
import depromeet.hotsix.obrit.receipt.client.OcrFailedException
import depromeet.hotsix.obrit.receipt.client.OcrFailureReason
import depromeet.hotsix.obrit.receipt.dto.OcrAnalysisResponse
import depromeet.hotsix.obrit.receipt.dto.OcrItem
import depromeet.hotsix.obrit.user.entity.UserFixture
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.AfterTransaction
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val DEFAULT_CATEGORY_ICON_ID = 1L

/**
 * 이벤트 적재는 REQUIRES_NEW라 테스트 트랜잭션 롤백에 걸리지 않는다.
 * 셋업 데이터만 롤백되고 이벤트는 커밋된 채 남으므로 정리를 두 군데서 한다.
 *
 * - [setUp]: 남의 이벤트가 이 테스트의 findAll()에 섞이지 않게 가린다(롤백돼도 무방).
 * - [deleteCommittedEvents]: 커밋된 이벤트를 실제로 지워 다음 테스트로 넘기지 않는다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReceiptAnalysisEventTest {

    @Autowired
    private lateinit var receiptService: ReceiptService

    @Autowired
    private lateinit var stubOcrService: StubOcrService

    @Autowired
    private lateinit var analyticsEventRepository: AnalyticsEventRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var categoryIconRepository: CategoryIconRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        // 앞선 테스트가 남긴 이벤트를 이 트랜잭션 안에서 안 보이게 한다.
        // 롤백되더라도 singleEvent()의 findAll()이 남의 이벤트를 세지 않게 하는 용도.
        analyticsEventRepository.deleteAll()
        stubOcrService.reset()

        val user = userRepository.save(UserFixture.user(id = null))
        userId = requireNotNull(user.id)

        // getDefaultCategoryIconUrl()이 아이콘 ID 1을 조회하므로 ID를 명시해 넣어둔다.
        // IDENTITY 전략이라 저장 순서로는 ID를 보장할 수 없다.
        if (categoryIconRepository.findById(DEFAULT_CATEGORY_ICON_ID).isEmpty) {
            jdbcTemplate.update(
                "INSERT INTO icons (id, name, icon_key, url, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                DEFAULT_CATEGORY_ICON_ID,
                "기본",
                "icons/default.png",
                "https://example.com/default.png",
            )
        }
    }

    @AfterEach
    fun tearDown() {
        stubOcrService.reset()
    }

    /**
     * 이벤트는 REQUIRES_NEW로 이미 커밋됐으므로 테스트 트랜잭션 안에서 지우면 롤백된다.
     * 트랜잭션이 끝난 뒤에 지워야 다음 테스트로 넘어가지 않는다.
     */
    @AfterTransaction
    fun deleteCommittedEvents() {
        analyticsEventRepository.deleteAll()
    }

    @Test
    fun `분석에 성공하면 success와_소요_시간과_인식_건수가_저장된다`() {
        stubOcrService.behavior = {
            OcrAnalysisResponse(
                date = "2026-01-01",
                items = listOf(OcrItem(original_name = "칫솔", category = "칫솔", effective_quantity = 2)),
            )
        }

        receiptService.analyzeReceipt(userId, imageFile())

        val event = singleEvent()
        assertEquals("receipt_analysis_finished", event.eventName)
        assertEquals(userId, event.userId)
        assertTrue(event.properties.contains("\"success\":true"), "properties=${event.properties}")
        assertTrue(event.properties.contains("\"detected_item_count\":1"), "properties=${event.properties}")
        assertTrue(event.properties.contains("\"failure_reason\":null"), "properties=${event.properties}")
        assertHasNonNullNumber(event.properties, "total_ms")
        assertHasNonNullNumber(event.properties, "ocr_ms")
    }

    @Test
    fun `OCR이 실패하면 실패_사유가_그대로_저장된다`() {
        stubOcrService.behavior = { throw OcrFailedException(OcrFailureReason.UPSTREAM_5XX, "AI API 서버 오류") }

        assertFailsWith<BusinessException> { receiptService.analyzeReceipt(userId, imageFile()) }

        val event = singleEvent()
        assertTrue(event.properties.contains("\"success\":false"), "properties=${event.properties}")
        assertTrue(event.properties.contains("\"failure_reason\":\"UPSTREAM_5XX\""), "properties=${event.properties}")
        assertTrue(event.properties.contains("\"detected_item_count\":null"), "properties=${event.properties}")
        assertHasNonNullNumber(event.properties, "ocr_ms")
    }

    @Test
    fun `허용되지_않은_확장자면 INVALID_FILE로_저장되고 ocr_ms는_없다`() {
        val pdf = MockMultipartFile("image", "receipt.pdf", "application/pdf", "dummy".toByteArray())

        assertFailsWith<BusinessException> { receiptService.analyzeReceipt(userId, pdf) }

        val event = singleEvent()
        assertTrue(event.properties.contains("\"success\":false"), "properties=${event.properties}")
        assertTrue(event.properties.contains("\"failure_reason\":\"INVALID_FILE\""), "properties=${event.properties}")
        assertTrue(event.properties.contains("\"ocr_ms\":null"), "properties=${event.properties}")
    }

    private fun imageFile() = MockMultipartFile("image", "receipt.jpg", "image/jpeg", "dummy".toByteArray())

    private fun singleEvent(): AnalyticsEventEntity {
        val saved = analyticsEventRepository.findAll()
        assertEquals(1, saved.size, "저장된 이벤트=$saved")
        return saved.first()
    }

    private fun assertHasNonNullNumber(properties: String, key: String) {
        assertTrue(
            Regex("\"$key\":\\d+").containsMatchIn(properties),
            "$key 가 숫자로 저장되지 않았습니다. properties=$properties",
        )
    }
}
