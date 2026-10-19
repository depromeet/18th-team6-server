package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.notification.dto.request.UpdateNotificationSettingsRequest
import depromeet.hotsix.obrit.notification.entity.NotificationPermissionStatus
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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserNotificationSettingsUpdateTest {

    @Autowired
    private lateinit var userNotificationSettingsService: UserNotificationSettingsService

    @Autowired
    private lateinit var userNotificationSettingsRepository: UserNotificationSettingsRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        userId = requireNotNull(userRepository.save(UserFixture.user(id = null)).id)
    }

    private fun update(
        preReplacementEnabled: Boolean = true,
        overdueEnabled: Boolean = true,
        lowStockEnabled: Boolean = true,
        dispatchTime: LocalTime = LocalTime.of(9, 0),
    ) = userNotificationSettingsService.updateSettings(
        userId,
        UpdateNotificationSettingsRequest(
            preReplacementEnabled = preReplacementEnabled,
            overdueEnabled = overdueEnabled,
            lowStockEnabled = lowStockEnabled,
            dispatchTime = dispatchTime,
        ),
    )

    @Test
    fun `설정 행이 없으면 저장 시 새로 만든다`() {
        update(dispatchTime = LocalTime.of(10, 30))

        assertEquals(LocalTime.of(10, 30), userNotificationSettingsRepository.findByUserId(userId)?.dispatchTime)
    }

    @Test
    fun `이미 저장한 설정이 있으면 같은 행을 갱신한다`() {
        update(dispatchTime = LocalTime.of(10, 30))
        update(dispatchTime = LocalTime.of(11, 0))

        assertEquals(1, userNotificationSettingsRepository.findAll().count { it.userId == userId })
        assertEquals(LocalTime.of(11, 0), userNotificationSettingsRepository.findByUserId(userId)?.dispatchTime)
    }

    @Test
    fun `저장한 값이 이후 조회에 반영된다`() {
        update(overdueEnabled = false, dispatchTime = LocalTime.of(20, 30))

        val settings = userNotificationSettingsService.effectiveSettings(userId)

        assertTrue(settings.preReplacementEnabled)
        assertFalse(settings.overdueEnabled)
        assertTrue(settings.lowStockEnabled)
        assertEquals(LocalTime.of(20, 30), settings.dispatchTime)
    }

    @Test
    fun `설정을 저장해도 권한 상태는 바뀌지 않는다`() {
        userNotificationSettingsRepository.save(
            UserNotificationSettings(userId = userId, permissionStatus = NotificationPermissionStatus.GRANTED),
        )

        update(overdueEnabled = false)

        assertEquals(
            NotificationPermissionStatus.GRANTED,
            userNotificationSettingsService.effectiveSettings(userId).permissionStatus,
        )
    }

    @Test
    fun `발송 시각이 30분 단위가 아니면 예외를 던진다`() {
        assertFailsWith<BusinessException> { update(dispatchTime = LocalTime.of(9, 15)) }
    }

    @Test
    fun `예전 조용 시간대의 알림 시간도 저장된다`() {
        assertEquals(LocalTime.of(23, 0), update(dispatchTime = LocalTime.of(23, 0)).dispatchTime)
        assertEquals(LocalTime.of(0, 0), update(dispatchTime = LocalTime.of(0, 0)).dispatchTime)
        assertEquals(LocalTime.of(7, 30), update(dispatchTime = LocalTime.of(7, 30)).dispatchTime)
    }
}
