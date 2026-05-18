package depromeet.hotsix.obrit.item.entity

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// 교체 시기
enum class ReplacementBand(val score: Int) {
    OVERDUE(0),
    WARN(1),
    SAFE(2),
    ;

    companion object {
        private const val WARN_DAYS = 3L

        fun of(today: LocalDate, nextReplacementDate: LocalDate): ReplacementBand {
            val daysUntil = ChronoUnit.DAYS.between(today, nextReplacementDate)

            return when {
                daysUntil <= 0 -> OVERDUE
                daysUntil <= WARN_DAYS -> WARN
                else -> SAFE
            }
        }
    }
}
