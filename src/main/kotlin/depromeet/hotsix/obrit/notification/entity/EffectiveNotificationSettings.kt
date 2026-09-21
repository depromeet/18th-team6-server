package depromeet.hotsix.obrit.notification.entity

import java.time.LocalTime

/**
 * 한 사용자에게 적용되는 알림 설정. 유저 설정이 있으면 그 값을, 없으면 전역 기본값을 담는다.
 *
 * 유형별 on/off는 유저 의도만 담는다. 전역 킬 스위치와 AND로 합치는 것은 판정 서비스의 책임이다.
 */
data class EffectiveNotificationSettings(
    val enabled: Boolean,
    val preReplacementEnabled: Boolean,
    val overdueEnabled: Boolean,
    val lowStockEnabled: Boolean,
    val leadDays: Int,
    val dispatchTime: LocalTime,
    val permissionStatus: NotificationPermissionStatus,
) {
    fun isEnabled(type: NotificationType): Boolean = enabled &&
        when (type) {
            NotificationType.PRE_REPLACEMENT -> preReplacementEnabled
            NotificationType.OVERDUE -> overdueEnabled
            NotificationType.LOW_STOCK -> lowStockEnabled
            // 공지는 정책 판정을 거치지 않으므로 유형별 on/off 대상이 아니다.
            NotificationType.NOTICE -> true
        }

    companion object {
        fun from(settings: UserNotificationSettings): EffectiveNotificationSettings = EffectiveNotificationSettings(
            enabled = settings.enabled,
            preReplacementEnabled = settings.preReplacementEnabled,
            overdueEnabled = settings.overdueEnabled,
            lowStockEnabled = settings.lowStockEnabled,
            leadDays = settings.leadDays,
            dispatchTime = settings.dispatchTime,
            permissionStatus = settings.permissionStatus,
        )

        /** 유형별 on/off는 켜짐으로 시작한다. 전역 값은 기본값이 아니라 킬 스위치이기 때문이다. */
        fun fallback(global: NotificationSettings): EffectiveNotificationSettings = EffectiveNotificationSettings(
            enabled = true,
            preReplacementEnabled = true,
            overdueEnabled = true,
            lowStockEnabled = true,
            leadDays = global.leadDays,
            dispatchTime = UserNotificationSettings.DEFAULT_DISPATCH_TIME,
            permissionStatus = NotificationPermissionStatus.NOT_REQUESTED,
        )
    }
}
