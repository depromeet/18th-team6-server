package depromeet.hotsix.obrit.notification.entity

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** 부분 실패와 기기없음의 처리 기준을 고정한다. */
class FcmSendResultTest {

    @Test
    fun `한 대라도 성공하면 발송으로 본다`() {
        assertEquals(FcmSendOutcome.SENT, FcmSendResult.of(sentCount = 1, failedCount = 1).outcome)
    }

    @Test
    fun `모든 기기가 실패하면 실패로 본다`() {
        assertEquals(FcmSendOutcome.FAILED, FcmSendResult.of(sentCount = 0, failedCount = 2).outcome)
    }

    @Test
    fun `보낸 기기도 실패한 기기도 없으면 기기없음으로 본다`() {
        assertEquals(FcmSendOutcome.NO_DEVICE, FcmSendResult.of(sentCount = 0, failedCount = 0).outcome)
    }

    @Test
    fun `기기가 등록돼 있지 않으면 실패가 아니라 기기없음이다`() {
        val result = FcmSendResult.noDevice()

        assertEquals(FcmSendOutcome.NO_DEVICE, result.outcome)
        assertEquals(0, result.failedCount)
    }
}
