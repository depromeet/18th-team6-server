package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.notification.repository.DeviceRegistrationRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.assertEquals

class NotificationDeepLinkServiceTest {

    @Test
    fun `설정된 앱 경로로 단건과 홈 딥링크를 생성한다`() {
        val service = NotificationDeepLinkService("example://consumables/{itemId}", "example://main")
        assertEquals("example://consumables/42", service.resolve(42))
        assertEquals("example://main", service.resolve(null))
    }

    @Test
    fun `FCM 메시지에 알림 ID와 딥링크를 함께 담는다`() {
        val service = FcmPushService(
            mock(DeviceRegistrationRepository::class.java),
            mock(FirebaseStatusService::class.java),
        )
        val data = mapOf("notificationId" to "12", "deepLink" to "obrit://items/42")
        val message = service.buildMessage("test-fid", "교체 시기가 지났어요", "수건 교체 예정일이 지났어요", data)
        assertEquals(data, ReflectionTestUtils.getField(message, "data"))
    }
}
