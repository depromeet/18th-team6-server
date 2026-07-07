package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.receipt.client.StubReceiptOcrClient
import depromeet.hotsix.obrit.receipt.dto.BatchOcrResponse
import depromeet.hotsix.obrit.receipt.dto.BatchOcrResult
import depromeet.hotsix.obrit.receipt.dto.OcrItem
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
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
        val jobId = receiptJobService.enqueue(userId = 1L, imageFile = image())

        val job = receiptJobRepository.findById(jobId).get()
        assertEquals(ReceiptJobStatus.PENDING, job.status)
        assertEquals("image/jpeg", job.mimeType)
        assertTrue(job.imageKey.isNotBlank())
    }

    @Test
    fun `pickBatch는_대기중_잡을_id_오름차순으로_선점해_PROCESSING으로_바꾼다`() {
        val first = receiptJobService.enqueue(userId = 1L, imageFile = image())
        val second = receiptJobService.enqueue(userId = 1L, imageFile = image())

        val picked = receiptJobService.pickBatch(maxBatch = 6)

        assertEquals(listOf(first, second), picked)
        assertEquals(ReceiptJobStatus.PROCESSING, receiptJobRepository.findById(first).get().status)
    }

    @Test
    fun `pickBatch는_maxBatch_개수만큼만_선점한다`() {
        repeat(3) { receiptJobService.enqueue(userId = 1L, imageFile = image()) }

        val picked = receiptJobService.pickBatch(maxBatch = 2)

        assertEquals(2, picked.size)
    }

    @Test
    fun `processBatch는_receipt_id로_매핑해_각_잡을_완료_처리한다`() {
        seedDefaultIcon()
        val user = userRepository.save(UserFixture.user())
        val jobId = receiptJobService.enqueue(userId = user.id!!, imageFile = image())
        stubReceiptOcrClient.batchResponse = BatchOcrResponse(listOf(successResult(jobId)))

        receiptJobService.processBatch(receiptJobService.pickBatch(maxBatch = 6))

        val job = receiptJobRepository.findById(jobId).get()
        assertEquals(ReceiptJobStatus.COMPLETED, job.status)
        assertNotNull(job.resultJson)
    }

    @Test
    fun `processBatch는_결과가_누락된_잡만_재시도하고_나머지는_완료한다`() {
        seedDefaultIcon()
        val user = userRepository.save(UserFixture.user())
        val completedId = receiptJobService.enqueue(userId = user.id!!, imageFile = image())
        val missingId = receiptJobService.enqueue(userId = user.id!!, imageFile = image())
        // completedId 결과만 응답에 포함 (missingId는 누락)
        stubReceiptOcrClient.batchResponse = BatchOcrResponse(listOf(successResult(completedId)))

        receiptJobService.processBatch(receiptJobService.pickBatch(maxBatch = 6))

        assertEquals(ReceiptJobStatus.COMPLETED, receiptJobRepository.findById(completedId).get().status)
        val missing = receiptJobRepository.findById(missingId).get()
        assertEquals(ReceiptJobStatus.PENDING, missing.status)
        assertEquals(1, missing.retryCount)
    }

    @Test
    fun `processBatch는_배치_호출_자체가_실패하면_전체_잡을_재시도한다`() {
        val first = receiptJobService.enqueue(userId = 1L, imageFile = image())
        val second = receiptJobService.enqueue(userId = 1L, imageFile = image())
        stubReceiptOcrClient.batchError = RuntimeException("배치 OCR 호출 실패")

        receiptJobService.processBatch(receiptJobService.pickBatch(maxBatch = 6))

        assertEquals(ReceiptJobStatus.PENDING, receiptJobRepository.findById(first).get().status)
        assertEquals(ReceiptJobStatus.PENDING, receiptJobRepository.findById(second).get().status)
    }

    @Test
    fun `releaseToPending은_선점한_잡들을_다시_대기중으로_되돌린다`() {
        receiptJobService.enqueue(userId = 1L, imageFile = image())
        receiptJobService.enqueue(userId = 1L, imageFile = image())
        val picked = receiptJobService.pickBatch(maxBatch = 6)

        receiptJobService.releaseToPending(picked)

        picked.forEach {
            assertEquals(ReceiptJobStatus.PENDING, receiptJobRepository.findById(it).get().status)
        }
    }

    @Test
    fun `getJob은_완료된_잡의_결과를_역직렬화해_반환한다`() {
        seedDefaultIcon()
        val user = userRepository.save(UserFixture.user())
        val jobId = receiptJobService.enqueue(userId = user.id!!, imageFile = image())
        stubReceiptOcrClient.batchResponse = BatchOcrResponse(listOf(successResult(jobId)))
        receiptJobService.processBatch(receiptJobService.pickBatch(maxBatch = 6))

        val response = receiptJobService.getJob(jobId)

        assertEquals(ReceiptJobStatus.COMPLETED, response.status)
        assertNotNull(response.result)
    }

    @Test
    fun `getJob은_존재하지_않는_잡이면_예외를_던진다`() {
        assertFailsWith<ResourceNotFoundException> {
            receiptJobService.getJob(99_999L)
        }
    }

    @Test
    fun `recoverStuckProcessing은_오래_PROCESSING_상태인_잡을_대기중으로_되돌린다`() {
        val stuckId = insertProcessingJob(updatedAtMinutesAgo = 2)

        receiptJobService.recoverStuckProcessing()

        assertEquals(ReceiptJobStatus.PENDING, receiptJobRepository.findById(stuckId).get().status)
    }

    @Test
    fun `recoverStuckProcessing은_최근_PROCESSING_잡은_건드리지_않는다`() {
        val recentId = insertProcessingJob(updatedAtMinutesAgo = 0)

        receiptJobService.recoverStuckProcessing()

        assertEquals(ReceiptJobStatus.PROCESSING, receiptJobRepository.findById(recentId).get().status)
    }

    private fun image() = MockMultipartFile("image", "receipt.jpg", "image/jpeg", "img".toByteArray())

    private fun successResult(jobId: Long) = BatchOcrResult(
        receiptId = jobId.toString(),
        store = "마트",
        date = "2026-01-01",
        items = listOf(OcrItem(original_name = "칫솔", category = "칫솔", effective_quantity = 1)),
        total = 1000,
    )

    private fun seedDefaultIcon() {
        jdbcTemplate.update(
            "INSERT INTO icons (id, name, icon_key, url, created_at, updated_at) " +
                "VALUES (1, 'default', 'icons/default.png', 'https://cdn.example.com/icons/default.png', " +
                "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        )
    }

    private fun insertProcessingJob(updatedAtMinutesAgo: Long): Long {
        val timestamp = LocalDateTime.now().minusMinutes(updatedAtMinutesAgo)
        jdbcTemplate.update(
            "INSERT INTO receipt_jobs (user_id, image_key, mime_type, status, retry_count, created_at, updated_at) " +
                "VALUES (1, 'receipts/stuck.jpg', 'image/jpeg', 'PROCESSING', 0, ?, ?)",
            timestamp,
            timestamp,
        )
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM receipt_jobs", Long::class.java)!!
    }
}
