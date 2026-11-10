package depromeet.hotsix.obrit.receipt.dto

import com.fasterxml.jackson.annotation.JsonProperty

/** 여러 영수증을 한 번에 분석한 배치 응답. 모델이 출력한 results 배열을 담는다. */
data class BatchOcrResponse(val results: List<BatchOcrResult> = emptyList())

/** 배치 응답의 개별 영수증 결과. receiptId로 원본 잡과 매핑한다. */
data class BatchOcrResult(
    @JsonProperty("receipt_id") val receiptId: String = "",
    val store: String? = null,
    val date: String? = null,
    val items: List<OcrItem> = emptyList(),
    val total: Long? = null,
)
