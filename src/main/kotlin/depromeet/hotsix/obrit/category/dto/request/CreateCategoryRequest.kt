package depromeet.hotsix.obrit.category.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class CreateCategoryRequest(
    @field:NotBlank
    val name: String,
    val imageUrl: String? = null,
    @field:Positive
    val defaultReplacementIntervalDays: Int,
)
