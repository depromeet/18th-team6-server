package depromeet.hotsix.obrit.global.log.analytics.event

data class ItemRegisteredDomainEvent(
    val userId: Long,
    val itemId: Long,
    val categoryId: Long,
    val replacementIntervalDays: Int,
    val source: String,
)
