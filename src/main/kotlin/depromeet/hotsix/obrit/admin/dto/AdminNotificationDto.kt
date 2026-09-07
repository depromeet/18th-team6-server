package depromeet.hotsix.obrit.admin.dto

import depromeet.hotsix.obrit.notification.entity.FirebaseInitializationState
import depromeet.hotsix.obrit.notification.entity.NotificationPreviewSnapshot

/** 푸시가 닿을 수 있는 사용자 비율. 발송 전에 이 값이 충분한지 먼저 확인한다. */
data class AdminDeviceCoverageRow(val totalUserCount: Long, val registeredUserCount: Long, val deviceCount: Long) {
    val coveragePercent: Double
        get() = if (totalUserCount == 0L) 0.0 else registeredUserCount * 100.0 / totalUserCount
}

data class AdminNotificationSettingsRow(
    val autoDispatchEnabled: Boolean,
    val leadDays: Int,
    val overdueStepDays: String,
    val preReplacementEnabled: Boolean,
    val overdueEnabled: Boolean,
    val lowStockEnabled: Boolean,
)

/**
 * Firebase 초기화 상태. 실패해도 기동은 되므로, 알림이 조용히 안 나가는 상황을 여기서 알아채야 한다.
 */
data class AdminFirebaseStatusRow(val state: FirebaseInitializationState, val failureReason: String?) {
    val healthy: Boolean
        get() = state == FirebaseInitializationState.INITIALIZED
}

data class AdminNotificationDashboard(
    val coverage: AdminDeviceCoverageRow,
    val firebase: AdminFirebaseStatusRow,
    val settings: AdminNotificationSettingsRow,
    val preview: NotificationPreviewSnapshot,
)

data class AdminNotificationSettingsForm(
    val leadDays: Int = 3,
    val overdueStepDays: String = "1,4,7",
    val preReplacementEnabled: Boolean = false,
    val overdueEnabled: Boolean = false,
    val lowStockEnabled: Boolean = false,
)

data class AdminNoticeForm(val title: String = "", val body: String = "", val userId: Long? = null)
