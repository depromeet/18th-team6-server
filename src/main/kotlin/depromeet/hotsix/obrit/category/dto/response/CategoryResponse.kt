package depromeet.hotsix.obrit.category.dto.response

import java.time.LocalDateTime

data class CategoryResponse(
    val id: Long,
    val name: String,
    val iconUrl: String,
    val userId: Long?,
    val createdAt: LocalDateTime?,
    val defaultReplacementIntervalDays: Int,
)
