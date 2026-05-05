package depromeet.hotsix.obrit.item.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.time.LocalDate

data class CreateItemRequest(
    @field:NotNull(message = "Category id is required.")
    val categoryId: Long,

    @field:NotBlank(message = "Item name is required.")
    val name: String,

    @field:PositiveOrZero(message = "Count must be zero or positive.")
    val count: Int,

    val lastReplacedDate: LocalDate? = null,

    @field:Positive(message = "Replacement interval days must be positive.")
    val replacementIntervalDays: Int? = null,
)

data class UpdateItemRequest(
    val name: String? = null,

    @field:PositiveOrZero(message = "Count must be zero or positive.")
    val count: Int? = null,

    val lastReplacedDate: LocalDate? = null,

    @field:Positive(message = "Replacement interval days must be positive.")
    val replacementIntervalDays: Int? = null,
)

data class CreateReplacementRequest(val replacedDate: LocalDate? = null)

data class ItemResponse(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val name: String,
    val count: Int,
    val replacementIntervalDays: Int,
    val lastReplacedDate: LocalDate,
    val nextReplacementDate: LocalDate,
)
