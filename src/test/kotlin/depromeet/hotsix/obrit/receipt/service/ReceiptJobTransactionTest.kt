package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.global.common.storage.StubFileUploader
import depromeet.hotsix.obrit.receipt.client.StubReceiptOcrClient
import depromeet.hotsix.obrit.receipt.dto.BatchOcrResponse
import depromeet.hotsix.obrit.receipt.dto.BatchOcrResult
import depromeet.hotsix.obrit.receipt.dto.OcrItem
import depromeet.hotsix.obrit.receipt.entity.ReceiptJobStatus
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * 테스트 트랜잭션 없이 [ReceiptJobService.processBatch]의 실제 커밋 결과를 검증한다.
 * 클래스에 `@Transactional`을 붙이면 잡별 트랜잭션이 테스트 트랜잭션에 합쳐져 검증 의미가 사라진다.
 * 폴링 스케줄러는 테스트 프로필에서 꺼져 있다(`obrit.scheduling.enabled=false`).
 */
@SpringBootTest
@ActiveProfiles("test")
class ReceiptJobTransactionTest {

    @Autowired
    private lateinit var receiptJobService: ReceiptJobService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var stubReceiptOcrClient: StubReceiptOcrClient

    @Autowired
    private lateinit var stubFileUploader: StubFileUploader

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
    fun `processBatch는_다운로드와_OCR_호출을_트랜잭션_밖에서_실행한다`() {
        val jobId = enqueueAndPick()
        stubReceiptOcrClient.batchResponse = BatchOcrResponse(listOf(successResult(jobId)))

        receiptJobService.processBatch(listOf(jobId))

        assertFalse(stubFileUploader.downloadCalledInTransaction!!)
        assertFalse(stubReceiptOcrClient.batchCalledInTransaction!!)
    }

    @Test
    fun `processBatch는_앞선_잡의_결과_반영이_실패해도_뒤_잡의_완료를_커밋한다`() {
        val normalId = enqueueAndPick()
        // 사용자 검증 이전에 쌓인 비정상 데이터: 결과 조립 시 사용자 조회에서 예외가 난다
        val brokenId = insertProcessingJob(userId = 99_999L, imageKey = imageKeyOf(normalId), retryCount = 0)
        stubReceiptOcrClient.batchResponse =
            BatchOcrResponse(listOf(successResult(brokenId), successResult(normalId)))

        receiptJobService.processBatch(listOf(brokenId, normalId))

        assertEquals(ReceiptJobStatus.COMPLETED.name, statusOf(normalId))
        assertEquals(ReceiptJobStatus.PENDING.name, statusOf(brokenId))
        assertEquals(1, retryCountOf(brokenId))
    }

    @Test
    fun `processBatch는_재시도를_소진한_잡의_결과_반영이_실패하면_FAILED로_커밋한다`() {
        val normalId = enqueueAndPick()
        val brokenId = insertProcessingJob(userId = 99_999L, imageKey = imageKeyOf(normalId), retryCount = 3)
        stubReceiptOcrClient.batchResponse = BatchOcrResponse(listOf(successResult(brokenId)))

        receiptJobService.processBatch(listOf(brokenId))

        assertEquals(ReceiptJobStatus.FAILED.name, statusOf(brokenId))
    }

    @Test
    fun `processBatch는_OCR_호출이_실패하면_각_잡의_재시도_상태를_커밋한다`() {
        val first = enqueueAndPick()
        val second = enqueueAndPick()
        stubReceiptOcrClient.batchError = RuntimeException("배치 OCR 호출 실패")

        receiptJobService.processBatch(listOf(first, second))

        listOf(first, second).forEach {
            assertEquals(ReceiptJobStatus.PENDING.name, statusOf(it))
            assertEquals(1, retryCountOf(it))
        }
    }

    private fun enqueueAndPick(): Long {
        val jobId = receiptJobService.enqueue(
            userId,
            MockMultipartFile("image", "receipt.jpg", "image/jpeg", "img".toByteArray()),
        )
        receiptJobService.pickBatch(maxBatch = 6)
        return jobId
    }

    private fun insertProcessingJob(userId: Long, imageKey: String, retryCount: Int): Long {
        jdbcTemplate.update(
            "INSERT INTO receipt_jobs (user_id, image_key, mime_type, status, retry_count, created_at, updated_at) " +
                "VALUES (?, ?, 'image/jpeg', 'PROCESSING', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            userId,
            imageKey,
            retryCount,
        )
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM receipt_jobs", Long::class.java)!!
    }

    private fun imageKeyOf(jobId: Long): String =
        jdbcTemplate.queryForObject("SELECT image_key FROM receipt_jobs WHERE id = ?", String::class.java, jobId)!!

    private fun statusOf(jobId: Long): String =
        jdbcTemplate.queryForObject("SELECT status FROM receipt_jobs WHERE id = ?", String::class.java, jobId)!!

    private fun retryCountOf(jobId: Long): Int =
        jdbcTemplate.queryForObject("SELECT retry_count FROM receipt_jobs WHERE id = ?", Int::class.java, jobId)!!

    private fun successResult(jobId: Long) = BatchOcrResult(
        receiptId = jobId.toString(),
        store = "마트",
        date = "2026-01-01",
        items = listOf(OcrItem(original_name = "칫솔", category = "칫솔", effective_quantity = 1)),
        total = 1000,
    )
}
