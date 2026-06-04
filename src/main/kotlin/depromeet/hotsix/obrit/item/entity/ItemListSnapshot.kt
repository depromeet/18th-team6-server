package depromeet.hotsix.obrit.item.entity

import java.time.LocalDate

data class ItemListSnapshot(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val quantity: Int,
    val lastReplacedDate: LocalDate,
    val nextReplacementDate: LocalDate,
) {

    fun replacementBand(today: LocalDate): ReplacementBand = ReplacementBand.of(today, nextReplacementDate)

    fun spareBand(): SpareBand = SpareBand.of(quantity)
}
