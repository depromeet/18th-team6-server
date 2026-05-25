package depromeet.hotsix.obrit.item.dto

import depromeet.hotsix.obrit.item.entity.ItemDetailStatus
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

@Schema(description = "소모품 여분 수량 수정 요청.")
data class UpdateSpareCountRequest(
    @field:Schema(description = "새 여분 수량. 0 이상이어야 합니다.", example = "3")
    @field:NotNull(message = "여분 수량은 필수입니다.")
    @field:PositiveOrZero(message = "여분 수량은 0 이상이어야 합니다.")
    val count: Int? = null,
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

@Schema(description = "소모품 상세 응답")
data class ItemDetailResponse(
    @field:Schema(description = "소모품 ID", example = "1")
    val id: Long,

    @field:Schema(description = "소모품명", example = "회사용 칫솔")
    val name: String,

    @field:Schema(description = "카테고리 정보")
    val category: ItemCategoryResponse,

    @field:Schema(description = "대표 이미지", example = "https://cdn.obrit.app/icons/toothbrush.png")
    val iconUrl: String,

    @field:Schema(description = "상태", example = "DANGER")
    val status: ItemDetailStatus,

    @field:Schema(description = "D-day 값", example = "-2")
    val dday: Int,

    @field:Schema(description = "표시용 라벨", example = "D+2")
    val ddayLabel: String,

    @field:Schema(description = "여분 수량", example = "0")
    val spareCount: Int,

    @field:Schema(description = "최근 교체일", example = "2026-03-03")
    val lastReplacedDate: LocalDate,

    @field:Schema(description = "다음 교체 예정일", example = "2026-04-03")
    val nextReplacementDate: LocalDate,

    @field:Schema(description = "며칠째 사용중", example = "31")
    val usedDays: Int,

    @field:Schema(description = "나의 평균 교체 주기(일)", example = "33.8")
    val myAverageCycleDays: Double,

    @field:Schema(description = "권장 교체 주기(일)", example = "30")
    val recommendedCycleDays: Int,

    @field:Schema(description = "사용 상태(0~100%, 100 초과 가능)", example = "103.3")
    val progressPercentage: Double,

    @field:Schema(description = "최근 5회 교체 기록")
    val recentReplacements: List<ItemReplacementResponse>,
)

@Schema(description = "소모품 카테고리 정보")
data class ItemCategoryResponse(
    @field:Schema(description = "카테고리 ID", example = "200")
    val id: Long,

    @field:Schema(description = "카테고리명", example = "칫솔")
    val name: String,
)

@Schema(description = "소모품 교체 기록")
data class ItemReplacementResponse(
    @field:Schema(description = "교체 기록 ID", example = "1")
    val id: Long,

    @field:Schema(description = "교체 일자", example = "2025-11-23")
    val date: LocalDate,

    @field:Schema(description = "해당 회차 주기(일)", example = "32")
    val cycleDays: Int,

    @field:Schema(description = "현재 사용중 여부", example = "false")
    val isCurrent: Boolean,
)

@Schema(description = "소모품 교체 이력 응답.")
data class ReplacementHistoryResponse(
    @field:Schema(description = "교체 이력 ID.", example = "42")
    val id: Long,

    @field:Schema(description = "교체일.", example = "2026-05-20")
    val replacedDate: LocalDate,
)
