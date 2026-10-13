package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.notification.DeviceRegistrationFixture
import depromeet.hotsix.obrit.notification.entity.FcmSendOutcome
import depromeet.hotsix.obrit.notification.repository.DeviceRegistrationRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals

class FcmInitializationFailureTest {
    @Test
    fun `초기화 실패는 기기 없음이 아닌 전송 실패로 반환한다`() {
        val repository = mock(DeviceRegistrationRepository::class.java)
        val status = FirebaseStatusService().apply { markFailed("credentials missing") }
        `when`(repository.findAllByUserId(1L))
            .thenReturn(listOf(DeviceRegistrationFixture.deviceRegistration(1L)))

        val result = FcmPushService(repository, status).sendToUser(1L, "제목", "본문")

        assertEquals(FcmSendOutcome.FAILED, result.outcome)
        assertEquals(0, result.sentCount)
        assertEquals(1, result.failedCount)
        verify(repository).findAllByUserId(1L)
        verifyNoMoreInteractions(repository)
    }
}
