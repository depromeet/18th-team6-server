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
import kotlin.test.assertNotNull

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
        enabled: Boolean = true,
        preReplacementEnabled: Boolean = true,
        overdueEnabled: Boolean = true,
        lowStockEnabled: Boolean = true,
        leadDays: Int = 3,
        dispatchTime: LocalTime = LocalTime.of(9, 0),
    ) = userNotificationSettingsService.updateSettings(
        userId,
        UpdateNotificationSettingsRequest(
            enabled = enabled,
            preReplacementEnabled = preReplacementEnabled,
            overdueEnabled = overdueEnabled,
            lowStockEnabled = lowStockEnabled,
            leadDays = leadDays,
            dispatchTime = dispatchTime,
        ),
    )

    @Test
    fun `설정 행이 없으면 저장 시 새로 만든다`() {
        update(leadDays = 5)

        assertEquals(5, userNotificationSettingsRepository.findByUserId(userId)?.leadDays)
    }

    @Test
    fun `이미 저장한 설정이 있으면 같은 행을 갱신한다`() {
        update(leadDays = 5)
        update(leadDays = 7)

        assertEquals(1, userNotificationSettingsRepository.findAll().count { it.userId == userId })
        assertEquals(7, userNotificationSettingsRepository.findByUserId(userId)?.leadDays)
    }

    @Test
    fun `저장한 값이 이후 조회에 반영된다`() {
        update(enabled = false, overdueEnabled = false, dispatchTime = LocalTime.of(20, 30))

        val settings = userNotificationSettingsService.effectiveSettings(userId)

        assertFalse(settings.enabled)
        assertFalse(settings.overdueEnabled)
        assertEquals(LocalTime.of(20, 30), settings.dispatchTime)
    }

    @Test
    fun `설정을 저장해도 권한 상태는 바뀌지 않는다`() {
        userNotificationSettingsRepository.save(
            UserNotificationSettings(userId = userId, permissionStatus = NotificationPermissionStatus.GRANTED),
        )

        update(enabled = false)

        assertEquals(
            NotificationPermissionStatus.GRANTED,
            userNotificationSettingsService.effectiveSettings(userId).permissionStatus,
        )
    }

    @Test
    fun `선행 일수가 허용 범위를 벗어나면 예외를 던진다`() {
        assertFailsWith<BusinessException> { update(leadDays = 0) }
        assertFailsWith<BusinessException> { update(leadDays = 8) }
    }

    @Test
    fun `발송 시각이 30분 단위가 아니면 예외를 던진다`() {
        assertFailsWith<BusinessException> { update(dispatchTime = LocalTime.of(9, 15)) }
    }

    @Test
    fun `발송 시각이 조용 시간에 걸치면 예외를 던진다`() {
        assertFailsWith<BusinessException> { update(dispatchTime = LocalTime.of(23, 0)) }
        assertFailsWith<BusinessException> { update(dispatchTime = LocalTime.of(7, 30)) }
    }

    @Test
    fun `조용 시간 경계값인 08시는 저장되고 22시는 거부된다`() {
        assertNotNull(update(dispatchTime = LocalTime.of(8, 0)))
        assertFailsWith<BusinessException> { update(dispatchTime = LocalTime.of(22, 0)) }
    }
}
