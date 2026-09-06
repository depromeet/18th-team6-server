package depromeet.hotsix.obrit.notification.entity

/** 알림 정책 판정 결과. 발송 확정 전 단계의 후보를 나타낸다. */
data class NotificationCandidate(
    val itemId: Long,
    val userId: Long,
    val itemName: String,
    val type: NotificationType,
    val daysUntil: Int,
)
