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

    @Schema(
        description = "등록용 추천 소모품 이름. 2단계 등록 시 CreateItemRequest.name 에 그대로 사용.",
        example = "닥터버들 2단미모 칫솔케어",
    )
    val suggestedName: String,

    @Schema(
        description = "매칭된 기존 카테고리 ID. " +
            "not null이면 2단계 CreateItemRequest.categoryId 에 전달. " +
            "null이면 suggestedCategoryName 을 CreateItemRequest.newCategoryName 으로 전달.",
        example = "300",
    )
    val categoryId: Long?,

    @Schema(
        description = "카테고리 아이콘 URL. categoryId 가 null(매칭 실패)이면 기본 아이콘 URL.",
        example = "https://cdn.example.com/icons/toothbrush.png",
    )
    val iconUrl: String,

    @Schema(
        description = "AI가 추출한 카테고리 이름. " +
            "categoryId 가 null 일 때 2단계 CreateItemRequest.newCategoryName 으로 사용.",
        example = "칫솔",
    )
    val suggestedCategoryName: String,

    @Schema(
        description = "추출된 수량. 2단계 CreateItemRequest.spareQuantity 에 그대로 사용.",
        example = "4",
    )
    val quantity: Int,

    @Schema(
        description = "추천 교체 주기(일). 매칭된 소모품은 그 소모품의 기본 주기, 매칭 실패 시 사용자 소모품 기본값 90. " +
            "2단계 등록 요청에 넘기지 않아도 서버가 같은 주기로 저장합니다.",
        example = "90",
    )
    val suggestedReplacementIntervalDays: Int,
)
