package depromeet.hotsix.obrit.item.entity

import java.time.LocalDate

enum class LastReplacementPeriod(private val daysAgo: Long) {
    WITHIN_WEEK(4),
    WITHIN_MONTH(21),
    WITHIN_THREE_MONTHS(45),
    OVER_THREE_MONTHS(90),
    ;

    fun toDate(today: LocalDate): LocalDate = today.minusDays(daysAgo)
}
