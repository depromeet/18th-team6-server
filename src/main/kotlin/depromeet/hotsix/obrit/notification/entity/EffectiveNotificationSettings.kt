package depromeet.hotsix.obrit.notification.entity

import java.time.LocalTime

/**
 * 한 사용자에게 적용되는 알림 설정. 유저 설정이 있으면 그 값을, 없으면 신규 사용자 기본값을 담는다.
 *
 * 유형별 on/off는 유저 의도만 담는다. 전역 킬 스위치와 AND로 합치는 것은 판정 서비스의 책임이다.
 */
data class EffectiveNotificationSettings(
    val preReplacementEnabled: Boolean,
    val overdueEnabled: Boolean,
    val lowStockEnabled: Boolean,
    val dispatchTime: LocalTime,
    val permissionStatus: NotificationPermissionStatus,
) {
    fun isEnabled(type: NotificationType): Boolean = when (type) {
        NotificationType.PRE_REPLACEMENT -> preReplacementEnabled
        NotificationType.OVERDUE -> overdueEnabled
        NotificationType.LOW_STOCK -> lowStockEnabled
        // 공지는 정책 판정을 거치지 않으므로 유형별 on/off 대상이 아니다.
        NotificationType.NOTICE -> true
    }

    companion object {
        fun from(settings: UserNotificationSettings): EffectiveNotificationSettings = EffectiveNotificationSettings(
            preReplacementEnabled = settings.preReplacementEnabled,
            overdueEnabled = settings.overdueEnabled,
            lowStockEnabled = settings.lowStockEnabled,
            dispatchTime = settings.dispatchTime,
            permissionStatus = settings.permissionStatus,
        )

        /** 설정 행이 없는 사용자. 새 사용자는 알림을 직접 켜기 전까지 받지 않는다 (PRD v2.1). */
        fun fallback(): EffectiveNotificationSettings = from(UserNotificationSettings(userId = 0))
    }
}
