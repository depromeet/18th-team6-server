package depromeet.hotsix.obrit.home.entity

import depromeet.hotsix.obrit.item.entity.ReplacementBand
import depromeet.hotsix.obrit.item.entity.SpareBand
import java.time.LocalDate

data class ItemSnapshot(val id: Long, val name: String, val nextReplacementDate: LocalDate, val quantity: Int) {

    // 교체 시기는?
    fun replacementBand(today: LocalDate): ReplacementBand = ReplacementBand.of(today, nextReplacementDate)

    // 여분 상태는?
    fun spareBand(): SpareBand = SpareBand.of(quantity)

    // 내 교체 점수는?
    fun replacementScore(today: LocalDate): Int = replacementBand(today).score

    // 내 여분 점수는?
    fun spareScore(): Int = when {
        quantity >= SPARE_GOOD_MIN -> SCORE_GOOD
        quantity >= SPARE_WARNING_MIN -> SCORE_WARNING
        else -> SCORE_DANGER
    }

    // 교체 시기가 지났는지?
    fun isReplacementOverdue(today: LocalDate): Boolean = replacementBand(today) == ReplacementBand.OVERDUE

    // 여분이 있는지?
    fun isSpareMissing(): Boolean = spareBand() == SpareBand.NONE

    companion object {
        private const val SCORE_DANGER = 0
        private const val SCORE_WARNING = 1
        private const val SCORE_GOOD = 2
        private const val SPARE_WARNING_MIN = 1
        private const val SPARE_GOOD_MIN = 3
    }
}
