package depromeet.hotsix.obrit.item

import depromeet.hotsix.obrit.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(
    name = "item_replacement_histories",
    indexes = [
        Index(name = "idx_item_replacement_histories_item_replaced", columnList = "item_id, replaced_date"),
    ],
)
class ItemReplacementHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    var item: Item,

    @Column(name = "replaced_date", nullable = false)
    var replacedDate: LocalDate,
) : BaseTimeEntity() {

    protected constructor() : this(
        item = Item(),
        replacedDate = LocalDate.EPOCH,
    )
}
