package depromeet.hotsix.obrit.receipt.entity

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ReceiptJobTest {

    @Test
    fun `markFailed는_컬럼_길이를_넘는_사유를_잘라_저장하고_FAILED로_바꾼다`() {
        val job = job()
        val message = "가".repeat(ReceiptJob.ERROR_MESSAGE_MAX_LENGTH + 1)

        job.markFailed(message)

        assertEquals(message.take(ReceiptJob.ERROR_MESSAGE_MAX_LENGTH), job.errorMessage)
        assertEquals(ReceiptJobStatus.FAILED, job.status)
    }

    @Test
    fun `markFailed는_컬럼_길이와_같은_사유는_그대로_저장한다`() {
        val job = job()
        val message = "가".repeat(ReceiptJob.ERROR_MESSAGE_MAX_LENGTH)

        job.markFailed(message)

        assertEquals(message, job.errorMessage)
    }

    @Test
    fun `retry는_컬럼_길이를_넘는_사유를_잘라_저장하고_재시도_횟수를_늘려_대기중으로_되돌린다`() {
        val job = job()
        val message = "가".repeat(ReceiptJob.ERROR_MESSAGE_MAX_LENGTH + 1)

        job.retry(message)

        assertEquals(message.take(ReceiptJob.ERROR_MESSAGE_MAX_LENGTH), job.errorMessage)
        assertEquals(1, job.retryCount)
        assertEquals(ReceiptJobStatus.PENDING, job.status)
    }

    @Test
    fun `retry는_컬럼_길이와_같은_사유는_그대로_저장한다`() {
        val job = job()
        val message = "가".repeat(ReceiptJob.ERROR_MESSAGE_MAX_LENGTH)

        job.retry(message)

        assertEquals(message, job.errorMessage)
    }

    private fun job() = ReceiptJob(userId = 1L, imageKey = "receipts/a.jpg", mimeType = "image/jpeg")
}
