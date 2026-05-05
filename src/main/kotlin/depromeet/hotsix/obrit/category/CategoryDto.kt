package depromeet.hotsix.obrit.category

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

fun Category.toResponse(): CategoryResponse = CategoryResponse(
    id = requireNotNull(id),
    name = name,
    imageUrl = imageUrl,
    defaultReplacementIntervalDays = defaultReplacementIntervalDays,
    preset = isPreset,
)
