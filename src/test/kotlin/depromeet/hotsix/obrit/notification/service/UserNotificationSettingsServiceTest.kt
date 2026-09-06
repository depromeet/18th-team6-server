package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.notification.entity.NotificationPermissionStatus
import depromeet.hotsix.obrit.notification.entity.NotificationType
import depromeet.hotsix.obrit.notification.entity.UserNotificationSettings
import depromeet.hotsix.obrit.notification.repository.UserNotificationSettingsRepository
import depromeet.hotsix.obrit.user.entity.UserFixture
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserNotificationSettingsServiceTest {

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

    @Test
    fun `설정을 저장한 적 없으면 전역 선행 일수를 기본값으로 반환한다`() {
        notificationSettingsService.update(
            leadDays = 5,
            overdueStepDays = "1,4,7",
            preReplacementEnabled = true,
            overdueEnabled = true,
            lowStockEnabled = true,
        )

        assertEquals(5, userNotificationSettingsService.effectiveSettings(userId).leadDays)
    }

    @Test
    fun `설정을 저장한 적 없으면 모든 유형이 켜진 상태로 반환한다`() {
        val settings = userNotificationSettingsService.effectiveSettings(userId)

        assertTrue(settings.enabled)
        assertTrue(NotificationType.entries.all { settings.isEnabled(it) })
    }

    @Test
    fun `전역에서 유형을 꺼도 유저 설정값은 켜진 상태로 반환한다`() {
        notificationSettingsService.update(
            leadDays = 3,
            overdueStepDays = "1,4,7",
            preReplacementEnabled = true,
            overdueEnabled = true,
            lowStockEnabled = false,
        )

        assertTrue(userNotificationSettingsService.effectiveSettings(userId).isEnabled(NotificationType.LOW_STOCK))
    }

    @Test
    fun `유저 설정이 있으면 전역 기본값 대신 유저 값을 반환한다`() {
        notificationSettingsService.update(
            leadDays = 5,
            overdueStepDays = "1,4,7",
            preReplacementEnabled = true,
            overdueEnabled = true,
            lowStockEnabled = true,
        )
        userNotificationSettingsRepository.save(UserNotificationSettings(userId = userId, leadDays = 7))

        assertEquals(7, userNotificationSettingsService.effectiveSettings(userId).leadDays)
    }

    @Test
    fun `전체 수신을 끄면 모든 유형이 꺼진 것으로 판정된다`() {
        userNotificationSettingsRepository.save(UserNotificationSettings(userId = userId, enabled = false))

        val settings = userNotificationSettingsService.effectiveSettings(userId)

        assertFalse(settings.isEnabled(NotificationType.PRE_REPLACEMENT))
        assertFalse(settings.isEnabled(NotificationType.OVERDUE))
        assertFalse(settings.isEnabled(NotificationType.LOW_STOCK))
    }

    @Test
    fun `유형을 끄면 해당 유형만 꺼진 것으로 판정된다`() {
        userNotificationSettingsRepository.save(
            UserNotificationSettings(userId = userId, overdueEnabled = false),
        )

        val settings = userNotificationSettingsService.effectiveSettings(userId)

        assertFalse(settings.isEnabled(NotificationType.OVERDUE))
        assertTrue(settings.isEnabled(NotificationType.PRE_REPLACEMENT))
        assertTrue(settings.isEnabled(NotificationType.LOW_STOCK))
    }

    @Test
    fun `설정을 저장한 적 없으면 발송 시각과 권한 상태가 기본값이다`() {
        val settings = userNotificationSettingsService.effectiveSettings(userId)

        assertEquals(LocalTime.of(9, 0), settings.dispatchTime)
        assertEquals(NotificationPermissionStatus.NOT_REQUESTED, settings.permissionStatus)
    }

    @Test
    fun `조회만으로는 유저 설정 행을 만들지 않는다`() {
        userNotificationSettingsService.getSettings(userId)

        assertEquals(null, userNotificationSettingsRepository.findByUserId(userId))
    }
}
