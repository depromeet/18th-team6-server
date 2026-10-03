package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.notification.dto.request.ReportNotificationPermissionRequest
import depromeet.hotsix.obrit.notification.entity.NotificationPermissionStatus
import depromeet.hotsix.obrit.notification.repository.UserNotificationSettingsRepository
import depromeet.hotsix.obrit.user.entity.UserFixture
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationPermissionReportTest {

    @Autowired
    private lateinit var userNotificationSettingsService: UserNotificationSettingsService

    @Autowired
    private lateinit var notificationSettingsService: NotificationSettingsService

    @Autowired
    private lateinit var userNotificationSettingsRepository: UserNotificationSettingsRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        userId = requireNotNull(userRepository.save(UserFixture.user(id = null)).id)
    }

    private fun report(status: NotificationPermissionStatus) =
        userNotificationSettingsService.reportPermission(userId, ReportNotificationPermissionRequest(status))

    @Test
    fun `설정 행이 없으면 보고 시 새로 만든다`() {
        report(NotificationPermissionStatus.GRANTED)

        assertEquals(
            NotificationPermissionStatus.GRANTED,
            userNotificationSettingsRepository.findByUserId(userId)?.permissionStatus,
        )
    }

    @Test
    fun `보고한 상태가 이후 조회에 반영된다`() {
        report(NotificationPermissionStatus.DENIED)

        assertEquals(
            NotificationPermissionStatus.DENIED,
            userNotificationSettingsService.effectiveSettings(userId).permissionStatus,
        )
    }

    @Test
    fun `다시 보고하면 같은 행의 상태를 갱신한다`() {
        report(NotificationPermissionStatus.DENIED)
        report(NotificationPermissionStatus.GRANTED)

        assertEquals(1, userNotificationSettingsRepository.findAll().count { it.userId == userId })
        assertEquals(
            NotificationPermissionStatus.GRANTED,
            userNotificationSettingsRepository.findByUserId(userId)?.permissionStatus,
        )
    }

    @Test
    fun `권한만 보고해도 알림 수신 설정은 켜진 상태로 남는다`() {
        val response = report(NotificationPermissionStatus.DENIED)

        assertTrue(response.enabled)
        assertTrue(response.preReplacementEnabled)
    }

    @Test
    fun `권한 보고로 만든 행의 선행 일수는 전역 값을 따른다`() {
        notificationSettingsService.update(
            leadDays = 5,
            overdueStepDays = "1,4,7",
            preReplacementEnabled = true,
            overdueEnabled = true,
            lowStockEnabled = true,
        )

        assertEquals(5, report(NotificationPermissionStatus.GRANTED).leadDays)
    }
}
