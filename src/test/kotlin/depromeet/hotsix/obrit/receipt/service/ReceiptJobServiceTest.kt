package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.receipt.client.StubReceiptOcrClient
import depromeet.hotsix.obrit.receipt.dto.OcrAnalysisResponse
import depromeet.hotsix.obrit.receipt.entity.ReceiptJobStatus
import depromeet.hotsix.obrit.receipt.repository.ReceiptJobRepository
import depromeet.hotsix.obrit.user.entity.UserFixture
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReceiptJobServiceTest {

    @Autowired
    private lateinit var receiptJobService: ReceiptJobService

    @Autowired
    private lateinit var receiptJobRepository: ReceiptJobRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var stubReceiptOcrClient: StubReceiptOcrClient

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setUp() {
        stubReceiptOcrClient.reset()
    }

    @Test
    fun `enqueue는_이미지를_저장하고_대기중_잡을_생성한다`() {
        val file = MockMultipartFile("image", "receipt.jpg", "image/jpeg", "img".toByteArray())

        val jobId = receiptJobService.enqueue(userId = 1L, imageFile = file)

        val job = receiptJobRepository.findById(jobId).get()
        assertEquals(ReceiptJobStatus.PENDING, job.status)
        assertEquals("image/jpeg", job.mimeType)
        assertTrue(job.imageKey.isNotBlank())
    }

    @Test
    fun `processNextPending은_성공하면_잡을_완료_처리하고_결과를_저장한다`() {
        seedDefaultIcon()
        val user = userRepository.save(UserFixture.user())
        val jobId = receiptJobService.enqueue(
            userId = user.id!!,
            imageFile = MockMultipartFile("image", "receipt.jpg", "image/jpeg", "img".toByteArray()),
        )
        stubReceiptOcrClient.response = OcrAnalysisResponse(date = "2026-01-01")

        receiptJobService.processNextPending()

        val job = receiptJobRepository.findById(jobId).get()
        assertEquals(ReceiptJobStatus.COMPLETED, job.status)
        assertNotNull(job.resultJson)
    }

    @Test
    fun `processNextPending은_OCR이_실패하면_잡을_실패_처리한다`() {
        val jobId = receiptJobService.enqueue(
            userId = 1L,
            imageFile = MockMultipartFile("image", "receipt.jpg", "image/jpeg", "img".toByteArray()),
        )
        stubReceiptOcrClient.error = RuntimeException("OCR 호출 실패")

        receiptJobService.processNextPending()

        val job = receiptJobRepository.findById(jobId).get()
        assertEquals(ReceiptJobStatus.FAILED, job.status)
        assertEquals("OCR 호출 실패", job.errorMessage)
    }

    private fun seedDefaultIcon() {
        jdbcTemplate.update(
            "INSERT INTO icons (id, name, icon_key, url, created_at, updated_at) " +
                "VALUES (1, 'default', 'icons/default.png', 'https://cdn.example.com/icons/default.png', " +
                "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        )
    }
}
