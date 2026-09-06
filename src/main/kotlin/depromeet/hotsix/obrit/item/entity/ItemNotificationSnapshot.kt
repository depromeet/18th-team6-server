package depromeet.hotsix.obrit.item.entity

import java.time.LocalDate

/**
 * 알림 정책 판정에 필요한 아이템 정보만 담는 읽기 전용 스냅샷.
 */
data class ItemNotificationSnapshot(
    val id: Long,
    val userId: Long,
    val name: String,
    val quantity: Int,
    val nextReplacementDate: LocalDate,
    val overdueNotifiedCount: Int,
    val lowStockNotifiedAt: LocalDate?,
)
