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

    @Column(name = "count", nullable = false)
    var quantity: Int,

    @Column(name = "replacement_interval_days", nullable = false)
    var replacementIntervalDays: Int,

    @Column(name = "last_replaced_date", nullable = false)
    var lastReplacedDate: LocalDate,

    @Column(name = "next_replacement_date", nullable = false)
    var nextReplacementDate: LocalDate,
) : BaseTimeEntity() {

    constructor() : this(
        userId = 0,
        categoryId = 0,
        name = "",
        quantity = 0,
        replacementIntervalDays = 1,
        lastReplacedDate = LocalDate.EPOCH,
        nextReplacementDate = LocalDate.EPOCH,
    )

    fun update(name: String?, quantity: Int?, replacementIntervalDays: Int?, lastReplacedDate: LocalDate?) {
        name?.let { this.name = it.trim() }
        quantity?.let { this.quantity = it }
        replacementIntervalDays?.let { this.replacementIntervalDays = it }
        lastReplacedDate?.let { this.lastReplacedDate = it }

        if (replacementIntervalDays != null || lastReplacedDate != null) {
            recalculateNextReplacementDate()
        }
    }

    fun updateSpareCount(quantity: Int) {
        this.quantity = quantity
    }

    fun replace(replacedDate: LocalDate) {
        lastReplacedDate = replacedDate
        recalculateNextReplacementDate()
    }

    private fun recalculateNextReplacementDate() {
        nextReplacementDate = lastReplacedDate.plusDays(replacementIntervalDays.toLong())
    }
}
