package depromeet.hotsix.obrit.global.log.analytics.event

import java.time.LocalDate

data class ReplacementRecordedDomainEvent(
    val userId: Long,
    val itemId: Long,
    val replacementHistoryId: Long,
    val replacedDate: LocalDate,
    val daysSinceLastReplacement: Int,
)
