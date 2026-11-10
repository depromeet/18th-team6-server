package depromeet.hotsix.obrit.receipt

import depromeet.hotsix.obrit.receipt.client.StubReceiptOcrClient
import depromeet.hotsix.obrit.receipt.dto.BatchOcrResponse
import depromeet.hotsix.obrit.receipt.dto.BatchOcrResult
import depromeet.hotsix.obrit.receipt.dto.OcrItem
import depromeet.hotsix.obrit.receipt.service.ReceiptJobService
import depromeet.hotsix.obrit.user.entity.UserFixture
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 클라이언트 계약: 등록 → 202 + Location → Location 폴링 → COMPLETED.
 * 스케줄러는 테스트 프로필에서 꺼져 있어 선점·처리를 직접 호출한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReceiptScanAcceptanceTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var receiptJobService: ReceiptJobService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var stubReceiptOcrClient: StubReceiptOcrClient

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private var userId = 0L

    @BeforeEach
    fun setUp() {
        stubReceiptOcrClient.reset()
        userId = userRepository.save(UserFixture.user()).id!!
        jdbcTemplate.update(
            "INSERT INTO icons (id, name, icon_key, url, created_at, updated_at) " +
                "VALUES (1, 'default', 'icons/default.png', 'https://cdn.example.com/icons/default.png', " +
                "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        )
    }

    @AfterEach
    fun tearDown() {
        jdbcTemplate.update("DELETE FROM receipt_jobs")
        jdbcTemplate.update("DELETE FROM icons WHERE id = 1")
        userRepository.deleteById(userId)
    }

    @Test
    fun `스캔을_등록하면_Location으로_폴링해_완료된_분석_결과를_받는다`() {
        val location = mockMvc.perform(
            multipart("/receipts/scans")
                .file(MockMultipartFile("image", "receipt.jpg", "image/jpeg", "img".toByteArray()))
                .header("X-User-Id", userId),
        )
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.data.scanId").isNumber)
            .andReturn().response.getHeader("Location")!!
        val scanId = location.substringAfterLast("/").toLong()

        stubReceiptOcrClient.batchResponse = BatchOcrResponse(listOf(successResult(scanId)))
        receiptJobService.processBatch(receiptJobService.pickBatch(maxBatch = 6))

        mockMvc.perform(get(location).header("X-User-Id", userId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.scanId").value(scanId))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
    }

    private fun successResult(scanId: Long) = BatchOcrResult(
        receiptId = scanId.toString(),
        store = "마트",
        date = "2026-01-01",
        items = listOf(OcrItem(original_name = "칫솔", category = "칫솔", effective_quantity = 1)),
        total = 1000,
    )
}
