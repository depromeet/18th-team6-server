package depromeet.hotsix.obrit.receipt.dto

import depromeet.hotsix.obrit.receipt.entity.ReceiptJobStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "영수증 스캔 상태 응답")
data class ReceiptScanResponse(
    @Schema(description = "스캔 ID", example = "1")
    val scanId: Long,

    @Schema(description = "스캔 상태 (PENDING/PROCESSING/COMPLETED/FAILED)", example = "COMPLETED")
    val status: ReceiptJobStatus,

    @Schema(description = "분석 결과. status가 COMPLETED일 때만 존재한다. 그대로 POST /items/bulk에 전달한다.")
    val result: AnalyzeReceiptResponse?,

    @Schema(description = "실패 사유. status가 FAILED일 때만 존재한다.", example = "AI API 호출 실패")
    val errorMessage: String?,
)
