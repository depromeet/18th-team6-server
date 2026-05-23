package depromeet.hotsix.obrit.item.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "Item creation request.")
data class CreateItemRequest(
    @field:Schema(description = "Category id for the item.", example = "200")
    @field:NotNull(message = "Category id is required.")
    val categoryId: Long,

    @field:Schema(description = "Item name.", example = "사무실 제로콜라")
    @field:NotBlank(message = "Item name is required.")
    val name: String,

    @field:Schema(description = "Current item count.", example = "12")
    @field:PositiveOrZero(message = "Count must be zero or positive.")
    val count: Int,

    @field:Schema(description = "Most recent replacement date.", example = "2026-04-20")
    val lastReplacedDate: LocalDate? = null,

    @field:Schema(
        description = "Custom replacement interval in days. Uses the category default when omitted.",
        example = "7",
    )
    @field:Positive(message = "Replacement interval days must be positive.")
    val replacementIntervalDays: Int? = null,
)

@Schema(description = "Item update request.")
data class UpdateItemRequest(
    @field:Schema(description = "New item name.", example = "집 제로콜라")
    val name: String? = null,

    @field:Schema(description = "New item count.", example = "6")
    @field:PositiveOrZero(message = "Count must be zero or positive.")
    val count: Int? = null,

    @field:Schema(description = "New most recent replacement date.", example = "2026-04-18")
    val lastReplacedDate: LocalDate? = null,

    @field:Schema(description = "New custom replacement interval in days.", example = "10")
    @field:Positive(message = "Replacement interval days must be positive.")
    val replacementIntervalDays: Int? = null,
)

@Schema(description = "Replacement record request.")
data class CreateReplacementRequest(
    @field:Schema(description = "Replacement date. Uses today when omitted.", example = "2026-04-25")
    val replacedDate: LocalDate? = null,
)

@Schema(description = "소모품 다건 등록 요청.")
data class BulkCreateItemRequest(
    @field:Schema(description = "등록할 소모품 목록. 최소 1개, 최대 10개.")
    @field:NotNull(message = "소모품 목록은 필수입니다.")
    @field:Size(min = 1, max = 10, message = "소모품 목록은 1개 이상 10개 이하여야 합니다.")
    @field:Valid
    val items: List<CreateItemRequest>,
)

@Schema(description = "Item response.")
data class ItemResponse(
    @field:Schema(description = "Item id.", example = "1")
    val id: Long,

    @field:Schema(description = "Category id.", example = "200")
    val categoryId: Long,

    @field:Schema(description = "Category name.", example = "제로콜라")
    val categoryName: String,

    @field:Schema(description = "Item name.", example = "사무실 제로콜라")
    val name: String,

    @field:Schema(description = "Current item count.", example = "12")
    val count: Int,

    @field:Schema(description = "Replacement interval in days.", example = "7")
    val replacementIntervalDays: Int,

    @field:Schema(description = "Most recent replacement date.", example = "2026-04-20")
    val lastReplacedDate: LocalDate,

    @field:Schema(description = "Next expected replacement date.", example = "2026-04-27")
    val nextReplacementDate: LocalDate,
)

@Schema(description = "소모품 교체 이력 응답.")
data class ReplacementHistoryResponse(
    @field:Schema(description = "교체 이력 ID.", example = "42")
    val id: Long,

    @field:Schema(description = "교체일.", example = "2026-05-20")
    val replacedDate: LocalDate,
)
