package depromeet.hotsix.obrit.category.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

@Schema(description = "Category creation request.")
data class CreateCategoryRequest(
    @field:Schema(description = "Category name.", example = "렌즈 세척액")
    @field:NotBlank(message = "Category name is required.")
    val name: String,

    @field:Schema(description = "Category image URL. Uses a default image when omitted.", example = "/images/lens.png")
    val imageUrl: String? = null,

    @field:Schema(description = "Default replacement interval in days.", example = "90")
    @field:Positive(message = "Default replacement interval days must be positive.")
    val defaultReplacementIntervalDays: Int,
)

@Schema(description = "Category response.")
data class CategoryResponse(
    @field:Schema(description = "Category id.", example = "100")
    val id: Long,

    @field:Schema(description = "Category name.", example = "면도기")
    val name: String,

    @field:Schema(description = "Category image URL.", example = "/images/default-category.png")
    val imageUrl: String,

    @field:Schema(description = "Default replacement interval in days.", example = "30")
    val defaultReplacementIntervalDays: Int,

    @field:Schema(description = "Whether this category is a preset category.", example = "true")
    val preset: Boolean,
)
