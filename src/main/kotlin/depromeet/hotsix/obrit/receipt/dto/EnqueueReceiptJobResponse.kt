package depromeet.hotsix.obrit.receipt.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "영수증 분석 잡 등록 응답")
data class EnqueueReceiptJobResponse(
    @Schema(description = "생성된 분석 잡 ID. 이 ID로 GET /receipts/jobs/{jobId}를 폴링한다.", example = "1")
    val jobId: Long,
)
