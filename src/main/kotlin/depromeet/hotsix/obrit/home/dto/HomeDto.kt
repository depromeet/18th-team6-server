package depromeet.hotsix.obrit.home.dto

import depromeet.hotsix.obrit.home.entity.HomeRiskBucket
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

@Schema(description = "위험도(위험/경고) 기준으로 분류된 아이템 버킷")
data class ItemBucketResponse(val bucket: HomeRiskBucket, val count: Int, val items: List<BucketItemResponse>)

@Schema(description = "홈 버킷에 표시되는 아이템")
data class BucketItemResponse(
    val itemId: Long,
    val name: String,
    val spareQuantity: Int,
    @field:Schema(description = "아이템 카테고리의 아이콘 URL", example = "https://cdn.example.com/icons/toothbrush.png")
    val iconUrl: String,
    val nextReplacementDate: LocalDate,
    val status: ItemStatus,
)

@Schema(description = "위험도(위험/경고) 기준으로 그룹화된 홈 버킷 목록. 항상 [DANGER, WARNING] 두 버킷이 반환됩니다.")
data class HomeBucketsResponse(val buckets: List<ItemBucketResponse>)

@Schema(description = "홈/리스트 화면의 무한 스크롤 목록에 표시되는 아이템 카드")
data class HomeItemCard(
    @field:Schema(description = "아이템 ID", example = "1001")
    val itemId: Long,
    @field:Schema(description = "아이템 이름", example = "칫솔")
    val name: String,
    @field:Schema(
        description = "아이템 카테고리의 아이콘 URL",
        example = "https://cdn.example.com/icons/toothbrush.png",
    )
    val iconUrl: String,
    @field:Schema(
        description = "마지막 교체일 이후 사용한 일수. 음수가 되지 않도록 0 미만은 0으로 보정됩니다.",
        example = "12",
    )
    val daysInUse: Int,
    @field:Schema(
        description = "교체 D-day 라벨. 형식: '교체 D-day' / '교체 D-{n}' / '교체 D+{n}'",
        example = "교체 D-3",
    )
    val replacementDday: String,
    @field:Schema(description = "여분 수량", example = "2")
    val spareQuantity: Int,
)
