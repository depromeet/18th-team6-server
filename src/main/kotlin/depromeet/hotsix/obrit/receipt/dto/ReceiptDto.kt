package depromeet.hotsix.obrit.receipt.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "영수증 분석 응답")
data class AnalyzeReceiptResponse(
    @Schema(description = "업로드된 영수증 이미지 URL", example = "https://...")
    val receiptImageUrl: String,

    @Schema(description = "영수증 구매일 (ISO 8601). 참고용.", example = "2026-04-23")
    val purchasedDate: String?,

    @Schema(description = "추출된 상품 목록")
    val items: List<AnalyzedItem>,
)

@Schema(description = "추출된 상품 정보")
data class AnalyzedItem(
    @Schema(description = "영수증 원문 상품명", example = "닥터버들 2단미모 칫솔케어 4개입")
    val originalName: String,

    @Schema(description = "등록용 추천 소모품 이름", example = "닥터버들 2단미모 칫솔케어")
    val suggestedName: String,

    @Schema(description = "매칭된 기존 카테고리 ID. 매칭 실패 시 null.", example = "300")
    val categoryId: Long?,

    @Schema(description = "AI가 추출한 카테고리 이름", example = "칫솔")
    val suggestedCategoryName: String,

    @Schema(description = "추출된 수량", example = "4")
    val quantity: Int,

    @Schema(description = "AI가 추정한 카테고리 권장 교체 주기(일)", example = "90")
    val suggestedReplacementIntervalDays: Int,
)
