package depromeet.hotsix.obrit.receipt.controller.docs

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.receipt.dto.EnqueueReceiptJobResponse
import depromeet.hotsix.obrit.receipt.dto.ReceiptJobResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.multipart.MultipartFile
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@Tag(name = "ReceiptJob", description = "영수증 비동기 분석 API")
interface ReceiptJobControllerApi {

    @Operation(
        summary = "영수증 분석 요청 (비동기)",
        description = """영수증 이미지를 업로드해 분석 잡을 큐에 등록하고 즉시 jobId를 반환합니다. (202 Accepted)

처리는 백그라운드에서 진행되며, 반환된 jobId로 GET /receipts/jobs/{jobId}를 폴링해 결과를 조회합니다.
동기 분석이 필요하면 기존 POST /receipts/analyze를 사용합니다.""",
        requestBody = SwaggerRequestBody(
            content = [Content(mediaType = "multipart/form-data")],
            description = "영수증 이미지 파일",
        ),
    )
    fun enqueue(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
        imageFile: MultipartFile,
    ): ApiResponse<EnqueueReceiptJobResponse>

    @Operation(
        summary = "영수증 분석 잡 상태 조회 (폴링)",
        description = """분석 잡의 상태와 결과를 조회합니다.

- PENDING/PROCESSING: 처리 중. result는 null입니다.
- COMPLETED: result에 분석 결과가 담깁니다. 그대로 POST /items/bulk에 전달합니다.
- FAILED: errorMessage에 실패 사유가 담깁니다.""",
    )
    fun getJob(
        @Parameter(description = "분석 잡 ID", required = true, example = "1")
        jobId: Long,
    ): ApiResponse<ReceiptJobResponse>
}
