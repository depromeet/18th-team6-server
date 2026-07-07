package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
        val jobId = receiptJobService.enqueue(userId = 1L, imageFile = image())

        val job = receiptJobRepository.findById(jobId).get()
        assertEquals(ReceiptJobStatus.PENDING, job.status)
        assertEquals("image/jpeg", job.mimeType)
        assertTrue(job.imageKey.isNotBlank())
    }

    @Test
    fun `pickNextPending은_대기중_잡을_선점해_PROCESSING으로_바꾸고_id를_반환한다`() {
        val jobId = receiptJobService.enqueue(userId = 1L, imageFile = image())

        val picked = receiptJobService.pickNextPending()

        assertEquals(jobId, picked)
        assertEquals(ReceiptJobStatus.PROCESSING, receiptJobRepository.findById(jobId).get().status)
    }

    @Test
    fun `pickNextPending은_대기중_잡이_없으면_null을_반환한다`() {
        assertNull(receiptJobService.pickNextPending())
    }

    @Test
    fun `process는_성공하면_잡을_완료_처리하고_결과를_저장한다`() {
        seedDefaultIcon()
        val user = userRepository.save(UserFixture.user())
        val jobId = receiptJobService.enqueue(userId = user.id!!, imageFile = image())
        stubReceiptOcrClient.response = OcrAnalysisResponse(date = "2026-01-01")

        receiptJobService.pickNextPending()
        receiptJobService.process(jobId)

        val job = receiptJobRepository.findById(jobId).get()
        assertEquals(ReceiptJobStatus.COMPLETED, job.status)
        assertNotNull(job.resultJson)
    }

    @Test
    fun `process는_실패해도_재시도_한도가_남으면_다시_대기중으로_되돌린다`() {
        val jobId = receiptJobService.enqueue(userId = 1L, imageFile = image())
        receiptJobService.pickNextPending()
        stubReceiptOcrClient.error = RuntimeException("OCR 호출 실패")

        receiptJobService.process(jobId)

        val job = receiptJobRepository.findById(jobId).get()
        assertEquals(ReceiptJobStatus.PENDING, job.status)
        assertEquals(1, job.retryCount)
        assertEquals("OCR 호출 실패", job.errorMessage)
    }

    @Test
    fun `process는_재시도_한도를_초과하면_잡을_실패_처리한다`() {
        val jobId = receiptJobService.enqueue(userId = 1L, imageFile = image())
        receiptJobService.pickNextPending()
        receiptJobRepository.findById(jobId).get().retryCount = MAX_RETRY
        stubReceiptOcrClient.error = RuntimeException("OCR 호출 실패")

        receiptJobService.process(jobId)

        val job = receiptJobRepository.findById(jobId).get()
        assertEquals(ReceiptJobStatus.FAILED, job.status)
        assertEquals(MAX_RETRY, job.retryCount)
    }

    @Test
    fun `releaseToPending은_선점한_잡을_다시_대기중으로_되돌린다`() {
        val jobId = receiptJobService.enqueue(userId = 1L, imageFile = image())
        receiptJobService.pickNextPending()

        receiptJobService.releaseToPending(jobId)

        assertEquals(ReceiptJobStatus.PENDING, receiptJobRepository.findById(jobId).get().status)
    }

    @Test
    fun `getJob은_완료된_잡의_결과를_역직렬화해_반환한다`() {
        seedDefaultIcon()
        val user = userRepository.save(UserFixture.user())
        val jobId = receiptJobService.enqueue(userId = user.id!!, imageFile = image())
        stubReceiptOcrClient.response = OcrAnalysisResponse(date = "2026-01-01")
        receiptJobService.pickNextPending()
        receiptJobService.process(jobId)

        val response = receiptJobService.getJob(jobId)

        assertEquals(ReceiptJobStatus.COMPLETED, response.status)
        assertNotNull(response.result)
        assertEquals("2026-01-01", response.result?.purchasedDate)
    }

    @Test
    fun `getJob은_존재하지_않는_잡이면_예외를_던진다`() {
        assertFailsWith<ResourceNotFoundException> {
            receiptJobService.getJob(99_999L)
        }
    }

    private fun image() = MockMultipartFile("image", "receipt.jpg", "image/jpeg", "img".toByteArray())

    private fun seedDefaultIcon() {
        jdbcTemplate.update(
            "INSERT INTO icons (id, name, icon_key, url, created_at, updated_at) " +
                "VALUES (1, 'default', 'icons/default.png', 'https://cdn.example.com/icons/default.png', " +
                "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        )
    }

    companion object {
        private const val MAX_RETRY = 3
    }
}
