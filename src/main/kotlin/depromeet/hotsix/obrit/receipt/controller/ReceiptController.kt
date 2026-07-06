package depromeet.hotsix.obrit.receipt.controller

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.receipt.dto.AnalyzeReceiptResponse
import depromeet.hotsix.obrit.receipt.service.ReceiptService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/receipts")
@Tag(name = "Receipt", description = "영수증 관련 API")
class ReceiptController(private val receiptService: ReceiptService) {

    @PostMapping("/analyze", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "영수증 분석",
        description = """영수증 이미지를 업로드하여 상품·카테고리·수량을 추출합니다. (소모품 등록 1단계)

응답 결과를 그대로 POST /items/bulk 에 전달하여 소모품을 일괄 등록합니다. (2단계)

**1단계 → 2단계 필드 매핑**
| 1단계 응답 (AnalyzedItem) | 2단계 요청 (CreateItemRequest) | 조건 |
|---|---|---|
| receiptImageUrl | BulkCreateItemRequest.receiptImageUrl | 항상 |
| categoryId (not null) | categoryId | 기존 카테고리가 매칭된 경우 |
| categoryId (null) → suggestedCategoryName | newCategoryName | 매칭 실패 시 새 카테고리 생성 |
| suggestedReplacementIntervalDays | newCategoryDefaultReplacementIntervalDays | newCategoryName 사용 시 함께 전달 |
| suggestedName | name | 항상 |
| quantity | spareQuantity | 항상 |""",
        requestBody = RequestBody(
            content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)],
            description = "영수증 이미지 파일",
        ),
    )
    fun analyzeReceipt(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestParam("image") imageFile: MultipartFile,
    ): ApiResponse<AnalyzeReceiptResponse> = ApiResponse.ok(receiptService.analyzeReceipt(userId, imageFile))
}
