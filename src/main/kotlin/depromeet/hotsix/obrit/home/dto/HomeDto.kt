package depromeet.hotsix.obrit.home.dto

import depromeet.hotsix.obrit.home.entity.ItemBucket
import depromeet.hotsix.obrit.home.entity.OverallStatus
import depromeet.hotsix.obrit.item.entity.ItemStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Home screen response.")
data class HomeResponse(
    val overallStatus: OverallStatusResponse,
    val myStatusSummary: MyStatusSummaryResponse,
    val itemBuckets: List<ItemBucketResponse>,
)

@Schema(description = "Overall home status.")
data class OverallStatusResponse(val replacement: ItemStatus, val spare: ItemStatus, val overall: OverallStatus)

@Schema(description = "My status summary.")
data class MyStatusSummaryResponse(val totalCount: Int, val needReplaceCount: Int, val score: Double)

@Schema(description = "Items grouped by spare and replacement status.")
data class ItemBucketResponse(val bucket: ItemBucket, val count: Int, val items: List<BucketItemResponse>)

@Schema(description = "Item shown in a home bucket.")
data class BucketItemResponse(
    val id: Long,
    val name: String,
    val count: Int,
    val nextReplacementDate: LocalDate,
    val status: ItemStatus,
)
