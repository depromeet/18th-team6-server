package depromeet.hotsix.obrit.global.readmodel

import java.time.LocalDate

data class ItemSnapshot(val id: Long, val name: String, val nextReplacementDate: LocalDate, val quantity: Int)
