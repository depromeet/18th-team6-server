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
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
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
        description = "영수증 이미지를 업로드하여 상품/카테고리/수량을 추출합니다.",
        requestBody = RequestBody(
            content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)],
            description = "영수증 이미지 파일",
        ),
    )
    fun analyzeReceipt(@RequestParam("image") imageFile: MultipartFile): ApiResponse<AnalyzeReceiptResponse> {
        val userId = SecurityContextHolder.getContext().authentication?.principal?.toString()?.toLong()
            ?: throw org.springframework.security.authentication.BadCredentialsException("인증 정보가 없습니다.")
        return ApiResponse.ok(receiptService.analyzeReceipt(userId, imageFile))
    }
}
