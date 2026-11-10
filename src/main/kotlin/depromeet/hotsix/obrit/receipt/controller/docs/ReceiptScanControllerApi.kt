package depromeet.hotsix.obrit.receipt.controller.docs

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.receipt.dto.CreateReceiptScanResponse
import depromeet.hotsix.obrit.receipt.dto.ReceiptScanResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@Tag(name = "ReceiptScan", description = "영수증 스캔(비동기 분석) API")
interface ReceiptScanControllerApi {

    @Operation(
        summary = "영수증 스캔 등록 (비동기)",
        description = """영수증 이미지를 업로드해 스캔을 등록하고 즉시 scanId를 반환합니다. (202 Accepted)

처리는 백그라운드에서 진행됩니다. 응답의 Location 헤더(GET /receipts/scans/{scanId})를 폴링해 결과를 조회합니다.
즉시 결과가 필요한 동기 분석은 POST /receipts/analyze입니다. 요청 형식이 같으니 경로를 혼동하지 않도록 주의하세요.""",
        requestBody = SwaggerRequestBody(
            content = [Content(mediaType = "multipart/form-data")],
            description = "영수증 이미지 파일",
        ),
    )
    fun createScan(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
        imageFile: MultipartFile,
    ): ResponseEntity<ApiResponse<CreateReceiptScanResponse>>

    @Operation(
        summary = "영수증 스캔 상태 조회 (폴링)",
        description = """스캔의 상태와 결과를 조회합니다.

- PENDING/PROCESSING: 처리 중. result는 null입니다.
- COMPLETED: result에 분석 결과가 담깁니다. 그대로 POST /items/bulk에 전달합니다.
- FAILED: errorMessage에 실패 사유가 담깁니다.

본인이 등록한 스캔만 조회할 수 있으며, 다른 사용자의 스캔은 존재하지 않는 스캔과 동일하게 404를 반환합니다.""",
    )
    fun getScan(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
        @Parameter(description = "스캔 ID", required = true, example = "1")
        scanId: Long,
    ): ApiResponse<ReceiptScanResponse>
}
