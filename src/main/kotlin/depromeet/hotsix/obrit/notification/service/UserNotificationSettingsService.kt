package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.notification.dto.request.UpdateNotificationSettingsRequest
import depromeet.hotsix.obrit.notification.dto.response.NotificationSettingsResponse
import depromeet.hotsix.obrit.notification.entity.EffectiveNotificationSettings
import depromeet.hotsix.obrit.notification.entity.NotificationSettings
import depromeet.hotsix.obrit.notification.entity.UserNotificationSettings
import depromeet.hotsix.obrit.notification.repository.UserNotificationSettingsRepository
import depromeet.hotsix.obrit.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

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

    /** 유저 설정을 저장한다. 행이 없으면 만든다. 권한 상태는 이 경로로 바꾸지 않는다. */
    @Transactional
    fun updateSettings(userId: Long, request: UpdateNotificationSettingsRequest): NotificationSettingsResponse {
        userService.validateUserExist(userId)
        validateLeadDays(request.leadDays)
        validateDispatchTime(request.dispatchTime)

        val settings = userNotificationSettingsRepository.findByUserId(userId)
            .let { it ?: UserNotificationSettings(userId = userId) }
            .apply {
                enabled = request.enabled
                preReplacementEnabled = request.preReplacementEnabled
                overdueEnabled = request.overdueEnabled
                lowStockEnabled = request.lowStockEnabled
                leadDays = request.leadDays
                dispatchTime = request.dispatchTime
            }

        return EffectiveNotificationSettings.from(userNotificationSettingsRepository.save(settings)).toResponse()
    }

    private fun validateLeadDays(leadDays: Int) {
        if (leadDays !in NotificationSettings.MIN_LEAD_DAYS..NotificationSettings.MAX_LEAD_DAYS) {
            throw BusinessException(
                "사전 알림 선행 일수는 ${NotificationSettings.MIN_LEAD_DAYS}일 이상 " +
                    "${NotificationSettings.MAX_LEAD_DAYS}일 이하여야 합니다.",
            )
        }
    }

    /**
     * 발송 시각을 검증한다.
     *
     * 조용 시간 값을 이월하지 않고 저장 단계에서 막는다. 23:00으로 둔 사용자는 매일 08:00으로 이월되어
     * 결과가 저장 제한과 같으므로, 상태를 늘리지 않는 쪽을 택했다.
     */
    private fun validateDispatchTime(dispatchTime: LocalTime) {
        if (dispatchTime.minute % UserNotificationSettings.DISPATCH_TIME_UNIT_MINUTES != 0 ||
            dispatchTime.second != 0 ||
            dispatchTime.nano != 0
        ) {
            throw BusinessException(
                "발송 시각은 ${UserNotificationSettings.DISPATCH_TIME_UNIT_MINUTES}분 단위로 설정해야 합니다.",
            )
        }
        if (dispatchTime < UserNotificationSettings.MIN_DISPATCH_TIME ||
            dispatchTime >= UserNotificationSettings.MAX_DISPATCH_TIME_EXCLUSIVE
        ) {
            throw BusinessException(
                "발송 시각은 ${UserNotificationSettings.MIN_DISPATCH_TIME} 이후 " +
                    "${UserNotificationSettings.MAX_DISPATCH_TIME_EXCLUSIVE} 이전이어야 합니다.",
            )
        }
    }

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

    /**
     * 여러 사용자의 설정을 한 번에 읽는다. 설정 행이 없는 사용자도 기본값으로 채워 반환한다.
     *
     * 배치는 소모품 전체를 훑으므로 건별로 조회하면 그만큼 쿼리가 늘어난다.
     */
    fun effectiveSettingsByUserIds(userIds: Set<Long>): Map<Long, EffectiveNotificationSettings> {
        if (userIds.isEmpty()) return emptyMap()

        val fallback = EffectiveNotificationSettings.fallback(notificationSettingsService.current())
        val saved = userNotificationSettingsRepository.findByUserIdIn(userIds)
            .associate { it.userId to EffectiveNotificationSettings.from(it) }

        return userIds.associateWith { saved[it] ?: fallback }
    }

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
