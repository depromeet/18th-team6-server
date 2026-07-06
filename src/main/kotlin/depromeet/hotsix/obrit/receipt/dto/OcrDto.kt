package depromeet.hotsix.obrit.receipt.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

data class OcrAnalysisResponse(
    val store: String? = null,
    val date: String? = null,
    val items: List<OcrItem> = emptyList(),
    val total: Long? = null,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class OcrItem(
    val original_name: String = "",
    val category: String = "",
    val effective_quantity: Int = 0,
    val quantity_reason: String = "",
    val unit_price: Long? = null,
    val total_price: Long? = null,
    val suggested_replacement_interval_days: Int? = null,
)
