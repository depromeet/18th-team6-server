package depromeet.hotsix.obrit.receipt.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "영수증 스캔 등록 응답")
data class CreateReceiptScanResponse(
    @Schema(description = "생성된 스캔 ID. 이 ID로 GET /receipts/scans/{scanId}를 폴링한다.", example = "1")
    val scanId: Long,
)
