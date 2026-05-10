package depromeet.hotsix.obrit.global.readmodel

import java.time.LocalDate

data class ItemListSnapshot(
    val id: Long,
    val name: String,
    val quantity: Int,
    val lastReplacedDate: LocalDate,
    val nextReplacementDate: LocalDate,
)
