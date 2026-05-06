package depromeet.hotsix.obrit.category.dto.response

data class CategoryResponse(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val defaultReplacementIntervalDays: Int,
    val preset: Boolean,
)
