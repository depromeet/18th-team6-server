package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.category.entity.Category
import depromeet.hotsix.obrit.category.repository.CategoryFixture
import depromeet.hotsix.obrit.category.repository.CategoryIconRepository
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.global.log.analytics.repository.AnalyticsEventRepository
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReceiptSuggestedIntervalTest {

    @Autowired
    private lateinit var receiptService: ReceiptService

    @Autowired
    private lateinit var stubOcrService: StubOcrService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var categoryIconRepository: CategoryIconRepository

    @Autowired
    private lateinit var analyticsEventRepository: AnalyticsEventRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        stubOcrService.reset()
        userId = requireNotNull(userRepository.save(UserFixture.user(id = null)).id)

        // getDefaultCategoryIconUrl()이 아이콘 ID 1을 조회한다. IDENTITY 전략이라 ID를 명시해 넣는다.
        if (categoryIconRepository.findById(Category.USER_DEFAULT_ICON_ID).isEmpty) {
            jdbcTemplate.update(
                "INSERT INTO icons (id, name, icon_key, url, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                Category.USER_DEFAULT_ICON_ID,
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

    /** 분석 완료 이벤트는 REQUIRES_NEW로 커밋되므로 트랜잭션이 끝난 뒤 지운다. */
    @AfterTransaction
    fun deleteCommittedEvents() {
        analyticsEventRepository.deleteAll()
    }

    @Test
    fun `매칭된_소모품은_그_소모품의_기본_주기를_추천하고_매칭_실패는_90일을_추천한다`() {
        categoryRepository.save(CategoryFixture.presetCategory(name = "칫솔", defaultReplacementIntervalDays = 30))
        stubOcrService.behavior = {
            OcrAnalysisResponse(
                date = "2026-10-01",
                items = listOf(
                    OcrItem(original_name = "칫솔 4입", category = "칫솔", suggested_replacement_interval_days = 7),
                    OcrItem(original_name = "고무장갑", category = "고무장갑", suggested_replacement_interval_days = 7),
                ),
            )
        }

        val result = receiptService.analyzeReceipt(userId, imageFile())

        val byName = result.items.associateBy { it.originalName }
        assertEquals(30, byName.getValue("칫솔 4입").suggestedReplacementIntervalDays)
        assertEquals(
            Category.USER_DEFAULT_REPLACEMENT_INTERVAL_DAYS,
            byName.getValue("고무장갑").suggestedReplacementIntervalDays,
        )
    }

    private fun imageFile() = MockMultipartFile("image", "receipt.jpg", "image/jpeg", "dummy".toByteArray())
}
