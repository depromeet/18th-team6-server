package depromeet.hotsix.obrit.home.dto

import depromeet.hotsix.obrit.home.entity.ItemBucket
import depromeet.hotsix.obrit.home.entity.OverallStatus
import depromeet.hotsix.obrit.item.entity.ItemStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "홈 화면, 1) 종합 상태 2) 내 상태 요약 3) 버킷 분류 ")
data class HomeResponse(
    val overallStatus: OverallStatusResponse,
    val myStatusSummary: MyStatusSummaryResponse,
    val itemBuckets: List<ItemBucketResponse>,
)

@Schema(description = "홈 화면 종합 상태")
data class OverallStatusResponse(val replacement: ItemStatus, val spare: ItemStatus, val overall: OverallStatus)

@Schema(description = "내 상태 요약")
data class MyStatusSummaryResponse(
    val totalCount: Int,
    val needReplaceCount: Int,
    val score: Double,
    val averageScore: Double,
)

@Schema(description = "여분/교체 상태 기준으로 분류된 아이템 버킷")
data class ItemBucketResponse(val bucket: ItemBucket, val count: Int, val items: List<BucketItemResponse>)

@Schema(description = "홈 버킷에 표시되는 아이템")
data class BucketItemResponse(
    val id: Long,
    val name: String,
    val count: Int,
    val nextReplacementDate: LocalDate,
    val status: ItemStatus,
)

@Schema(description = "여분/교체 상태 기준으로 그룹화된 홈 버킷 목록")
data class HomeBucketsResponse(val buckets: List<ItemBucketResponse>)
