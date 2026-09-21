package depromeet.hotsix.obrit.notification.entity

import java.time.LocalDate

/** 알림 정책 판정 결과. 발송 확정 전 단계의 후보를 나타낸다. */
data class NotificationCandidate(
    val itemId: Long,
    val userId: Long,
    val itemName: String,
    val type: NotificationType,
    val daysUntil: Int,
    val nextReplacementDate: LocalDate,
) {
    fun label(): String = when (type) {
        NotificationType.LOW_STOCK -> "여분 부족"
        NotificationType.PRE_REPLACEMENT, NotificationType.OVERDUE -> when {
            daysUntil > 0 -> "교체 D-$daysUntil"
            daysUntil < 0 -> "교체 D+${-daysUntil}"
            else -> "교체 D-day"
        }
        NotificationType.NOTICE -> error("공지는 정책 판정 대상이 아니다.")
    }
}
