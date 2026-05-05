package depromeet.hotsix.obrit.category.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class CreateCategoryRequest(
    @field:NotBlank(message = "Category name is required.")
    val name: String,

    val imageUrl: String? = null,

    @field:Positive(message = "Default replacement interval days must be positive.")
    val defaultReplacementIntervalDays: Int,
)

data class CategoryResponse(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val defaultReplacementIntervalDays: Int,
    val preset: Boolean,
)
