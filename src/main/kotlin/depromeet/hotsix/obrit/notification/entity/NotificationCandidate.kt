package depromeet.hotsix.obrit.notification.entity

import java.time.LocalDate

/**
 * 알림 정책 판정 결과. 발송 확정 전 단계의 후보를 나타낸다.
 *
 * 한 소모품의 사유는 일정 사유(사전·당일 또는 지연) 최대 1개와 여분 부족 최대 1개다.
 * [reasons]는 일정 사유, 여분 부족 순이며 비어 있지 않다.
 */
data class NotificationCandidate(
    val itemId: Long,
    val userId: Long,
    val itemName: String,
    val reasons: List<NotificationType>,
    val daysUntil: Int,
    val nextReplacementDate: LocalDate,
) {
    init {
        require(reasons.isNotEmpty()) { "알림 후보에는 사유가 하나 이상 있어야 한다." }
    }

    /** 알림함에 남기는 대표 유형. */
    val primaryType: NotificationType
        get() = reasons.first()

    /** 일정 사유(사전·당일 또는 지연). 여분 부족만 있으면 null. */
    val scheduleReason: NotificationType?
        get() = reasons.firstOrNull { it != NotificationType.LOW_STOCK }

    val hasLowStock: Boolean
        get() = NotificationType.LOW_STOCK in reasons

    fun label(): String = listOfNotNull(
        scheduleReason?.let { scheduleLabel() },
        if (hasLowStock) "여분 부족" else null,
    ).joinToString(" · ")

    private fun scheduleLabel(): String = when {
        daysUntil > 0 -> "교체 D-$daysUntil"
        daysUntil < 0 -> "교체 D+${-daysUntil}"
        else -> "교체 D-day"
    }
}
