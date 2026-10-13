package depromeet.hotsix.obrit.item.entity

import depromeet.hotsix.obrit.global.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(
    name = "items",
    indexes = [
        Index(name = "idx_items_user_deleted_next", columnList = "user_id, deleted_at, next_replacement_date"),
        Index(name = "idx_items_category_deleted", columnList = "category_id, deleted_at"),
    ],
)
class Item(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "category_id", nullable = false)
    var categoryId: Long,

    @Column(nullable = false)
    var name: String,

    /** 여분 수량. null은 미입력이고 0은 사용자가 명시한 "여분 없음"이다. 둘은 알림 판정에서 다르게 다룬다. */
    @Column(name = "count")
    var quantity: Int? = null,

    @Column(name = "replacement_interval_days", nullable = false)
    var replacementIntervalDays: Int,

    @Column(name = "last_replaced_date", nullable = false)
    var lastReplacedDate: LocalDate,

    @Column(name = "next_replacement_date", nullable = false)
    var nextReplacementDate: LocalDate,

    @Column(name = "receipt_image_url", length = 512)
    var receiptImageUrl: String? = null,

    @Column(name = "overdue_notified_count", nullable = false)
    var overdueNotifiedCount: Int = 0,

    @Column(name = "last_overdue_notified_at")
    var lastOverdueNotifiedAt: LocalDate? = null,

    @Column(name = "low_stock_notified_at")
    var lowStockNotifiedAt: LocalDate? = null,
) : BaseTimeEntity() {

    constructor() : this(
        userId = 0,
        categoryId = 0,
        name = "",
        quantity = null,
        replacementIntervalDays = 1,
        lastReplacedDate = LocalDate.EPOCH,
        nextReplacementDate = LocalDate.EPOCH,
        receiptImageUrl = null,
        overdueNotifiedCount = 0,
        lastOverdueNotifiedAt = null,
        lowStockNotifiedAt = null,
    )

    fun update(name: String?, quantity: Int?, replacementIntervalDays: Int?, lastReplacedDate: LocalDate?) {
        name?.let { this.name = it.trim() }
        quantity?.let { this.quantity = it }
        replacementIntervalDays?.let { this.replacementIntervalDays = it }
        lastReplacedDate?.let { this.lastReplacedDate = it }

        if (replacementIntervalDays != null || lastReplacedDate != null) {
            recalculateNextReplacementDate()
            resetOverdueNotification()
        }
        if (quantity != null) {
            resetLowStockNotificationIfRestocked()
        }
    }

    fun updateSpareCount(quantity: Int?) {
        this.quantity = quantity
        resetLowStockNotificationIfRestocked()
    }

    fun replace(replacedDate: LocalDate) {
        lastReplacedDate = replacedDate
        // 미입력은 차감하지 않고 미입력으로 둔다. 모르는 값을 0으로 확정하면 안 된다.
        quantity = quantity?.let { (it - 1).coerceAtLeast(0) }
        recalculateNextReplacementDate()
        resetOverdueNotification()
        resetLowStockNotificationIfRestocked()
    }

    /** 지연 알림 발송 시 호출한다. D+1/D+4/D+7 스텝 진행에 맞춰 마지막 발송일만 갱신한다. */
    fun recordOverdueNotification(notifiedAt: LocalDate) {
        overdueNotifiedCount += 1
        lastOverdueNotifiedAt = notifiedAt
    }

    /** 여분 부족 알림 발송 시 호출한다. 재입고 전까지 다시 발송하지 않는다. */
    fun recordLowStockNotification(notifiedAt: LocalDate) {
        lowStockNotifiedAt = notifiedAt
    }

    private fun resetOverdueNotification() {
        overdueNotifiedCount = 0
        lastOverdueNotifiedAt = null
    }

    private fun resetLowStockNotificationIfRestocked() {
        // 미입력으로 되돌아간 경우도 알림 대상이 아니므로 함께 초기화한다.
        val current = quantity
        if (current == null || current > 0) {
            lowStockNotifiedAt = null
        }
    }

    private fun recalculateNextReplacementDate() {
        nextReplacementDate = lastReplacedDate.plusDays(replacementIntervalDays.toLong())
    }
}
