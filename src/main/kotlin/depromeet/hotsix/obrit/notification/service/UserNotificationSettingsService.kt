package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.notification.dto.request.ReportNotificationPermissionRequest
import depromeet.hotsix.obrit.notification.dto.response.NotificationSettingsResponse
import depromeet.hotsix.obrit.notification.entity.EffectiveNotificationSettings
import depromeet.hotsix.obrit.notification.entity.UserNotificationSettings
import depromeet.hotsix.obrit.notification.repository.UserNotificationSettingsRepository
import depromeet.hotsix.obrit.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 유저별 알림 설정 조회. 유저 설정이 없으면 전역 기본값으로 내려간다. */
@Service
class UserNotificationSettingsService(
    private val userNotificationSettingsRepository: UserNotificationSettingsRepository,
    private val notificationSettingsService: NotificationSettingsService,
    private val userService: UserService,
) {

    fun getSettings(userId: Long): NotificationSettingsResponse {
        userService.validateUserExist(userId)

        return effectiveSettings(userId).toResponse()
    }

    /** 기기 알림 권한 상태를 기록한다. 설정 행이 없으면 만든다. */
    @Transactional
    fun reportPermission(userId: Long, request: ReportNotificationPermissionRequest): NotificationSettingsResponse {
        userService.validateUserExist(userId)

        val settings = userNotificationSettingsRepository.findByUserId(userId)
            .let { it ?: UserNotificationSettings(userId = userId, leadDays = defaultLeadDays()) }
            .apply { permissionStatus = request.permissionStatus }

        return EffectiveNotificationSettings.from(userNotificationSettingsRepository.save(settings)).toResponse()
    }

    /** 권한만 보고한 사용자의 선행 일수가 보고 전후로 달라지지 않도록 전역 값을 가져온다. */
    private fun defaultLeadDays() = notificationSettingsService.current().leadDays

    /**
     * 이 사용자에게 적용되는 설정값. 배치 판정도 같은 경로를 쓴다.
     *
     * readOnly 트랜잭션으로 감싸지 않는다. [NotificationSettingsService.current]가 전역 설정 행이 없으면
     * 만들어 반환하므로, 읽기 전용 경계 안에서는 시드되지 않은 환경에서 저장이 실패한다.
     */
    fun effectiveSettings(userId: Long): EffectiveNotificationSettings =
        userNotificationSettingsRepository.findByUserId(userId)
            ?.let { EffectiveNotificationSettings.from(it) }
            ?: EffectiveNotificationSettings.fallback(notificationSettingsService.current())

    private fun EffectiveNotificationSettings.toResponse() = NotificationSettingsResponse(
        enabled = enabled,
        preReplacementEnabled = preReplacementEnabled,
        overdueEnabled = overdueEnabled,
        lowStockEnabled = lowStockEnabled,
        leadDays = leadDays,
        dispatchTime = dispatchTime,
        permissionStatus = permissionStatus,
    )
}
