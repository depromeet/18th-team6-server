package depromeet.hotsix.obrit.home.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

// 개별 항목 상태
enum class ItemStatus {
    GOOD,
    WARNING,
    DANGER,
}

// 전체 종합 상태
enum class OverallStatus {
    PERFECT,
    GOOD,
    WARNING,
    DANGER,
}

// 아이템 분류 버킷 6종류
enum class ItemBucket(val priority: Int, val status: ItemStatus) {
    NONE_OVERDUE(1, ItemStatus.DANGER),
    NONE_WARN(2, ItemStatus.DANGER),
    HAS_OVERDUE(3, ItemStatus.DANGER),
    HAS_WARN(4, ItemStatus.WARNING),
    NONE_SAFE(5, ItemStatus.WARNING),
    HAS_SAFE(6, ItemStatus.GOOD),
}

// 교체 시기
enum class ReplacementBand {
    OVERDUE,
    WARN,
    SAFE,
}

// 여분 유무
enum class SpareBand {
    NONE,
    HAS,
}

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
