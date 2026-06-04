package depromeet.hotsix.obrit.category.entity

data class CategorySnapshot(
    val id: Long,
    val name: String,
    val iconUrl: String,
    val defaultReplacementIntervalDays: Int,
)
