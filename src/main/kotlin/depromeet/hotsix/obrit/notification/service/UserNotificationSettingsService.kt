package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.notification.dto.request.UpdateNotificationSettingsRequest
import depromeet.hotsix.obrit.notification.dto.response.NotificationSettingsResponse
import depromeet.hotsix.obrit.notification.entity.EffectiveNotificationSettings
import depromeet.hotsix.obrit.notification.entity.UserNotificationSettings
import depromeet.hotsix.obrit.notification.repository.UserNotificationSettingsRepository
import depromeet.hotsix.obrit.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

/** 유저별 알림 설정 조회. 유저 설정이 없으면 신규 사용자 기본값(모두 꺼짐)으로 내려간다. */
@Service
class UserNotificationSettingsService(
    private val userNotificationSettingsRepository: UserNotificationSettingsRepository,
    private val userService: UserService,
) {

    fun getSettings(userId: Long): NotificationSettingsResponse {
        userService.validateUserExist(userId)

        return effectiveSettings(userId).toResponse()
    }

    /** 유저 설정을 저장한다. 행이 없으면 만든다. 권한 상태는 이 경로로 바꾸지 않는다. */
    @Transactional
    fun updateSettings(userId: Long, request: UpdateNotificationSettingsRequest): NotificationSettingsResponse {
        userService.lockForSettingsUpdate(userId)
        validateDispatchTime(request.dispatchTime)

        val settings = userNotificationSettingsRepository.findByUserId(userId)
            .let { it ?: UserNotificationSettings(userId = userId) }
            .apply {
                preReplacementEnabled = request.preReplacementEnabled
                overdueEnabled = request.overdueEnabled
                lowStockEnabled = request.lowStockEnabled
                dispatchTime = request.dispatchTime
            }

        return EffectiveNotificationSettings.from(userNotificationSettingsRepository.save(settings)).toResponse()
    }

    /** 알림 시간은 30분 단위만 받는다. 시간대 제한(조용 시간)은 v2.1에서 없앴다. */
    private fun validateDispatchTime(dispatchTime: LocalTime) {
        if (dispatchTime.minute % UserNotificationSettings.DISPATCH_TIME_UNIT_MINUTES != 0 ||
            dispatchTime.second != 0 ||
            dispatchTime.nano != 0
        ) {
            throw BusinessException(
                "발송 시각은 ${UserNotificationSettings.DISPATCH_TIME_UNIT_MINUTES}분 단위로 설정해야 합니다.",
            )
        }
    }

    /** 이 사용자에게 적용되는 설정값. 배치 판정도 같은 경로를 쓴다. */
    fun effectiveSettings(userId: Long): EffectiveNotificationSettings =
        userNotificationSettingsRepository.findByUserId(userId)
            ?.let { EffectiveNotificationSettings.from(it) }
            ?: EffectiveNotificationSettings.fallback()

    /**
     * 여러 사용자의 설정을 한 번에 읽는다. 설정 행이 없는 사용자도 기본값으로 채워 반환한다.
     *
     * 배치는 소모품 전체를 훑으므로 건별로 조회하면 그만큼 쿼리가 늘어난다.
     */
    fun effectiveSettingsByUserIds(userIds: Set<Long>): Map<Long, EffectiveNotificationSettings> {
        if (userIds.isEmpty()) return emptyMap()

        val fallback = EffectiveNotificationSettings.fallback()
        val saved = userNotificationSettingsRepository.findByUserIdIn(userIds)
            .associate { it.userId to EffectiveNotificationSettings.from(it) }

        return userIds.associateWith { saved[it] ?: fallback }
    }

    private fun EffectiveNotificationSettings.toResponse() = NotificationSettingsResponse(
        preReplacementEnabled = preReplacementEnabled,
        overdueEnabled = overdueEnabled,
        lowStockEnabled = lowStockEnabled,
        dispatchTime = dispatchTime,
        permissionStatus = permissionStatus,
    )
}
