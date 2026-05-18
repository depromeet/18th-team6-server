package depromeet.hotsix.obrit.home.service

import depromeet.hotsix.obrit.home.dto.BucketItemResponse
import depromeet.hotsix.obrit.home.dto.HomeResponse
import depromeet.hotsix.obrit.home.dto.ItemBucketResponse
import depromeet.hotsix.obrit.home.dto.MyStatusSummaryResponse
import depromeet.hotsix.obrit.home.dto.OverallStatusResponse
import depromeet.hotsix.obrit.home.entity.ItemBucket
import depromeet.hotsix.obrit.home.entity.ItemSnapshot
import depromeet.hotsix.obrit.home.entity.OverallStatus
import depromeet.hotsix.obrit.item.entity.ItemStatus
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class HomeStatusCalculatorService {

    companion object {
        private const val MAX_SCORE = 2.0
        private const val DANGER_RATIO_WARNING_LIMIT = 0.3
        private const val REPLACEMENT_AVERAGE_WARNING_MIN = 1.0
        private const val DEFAULT_AVERAGE_SCORE = 45.0
        private const val REPLACEMENT_SCORE_WEIGHT = 0.6
        private const val SPARE_SCORE_WEIGHT = 0.4
    }

    fun calculate(today: LocalDate, items: List<ItemSnapshot>): HomeResponse = HomeResponse(
        overallStatus = calculateOverallStatus(today, items),
        myStatusSummary = calculateMyStatusSummary(today, items),
        itemBuckets = bucketize(today, items),
    )

    fun calculateOverallStatus(today: LocalDate, items: List<ItemSnapshot>): OverallStatusResponse {
        if (items.isEmpty()) {
            return OverallStatusResponse(
                replacement = ItemStatus.GOOD,
                spare = ItemStatus.GOOD,
                overall = OverallStatus.PERFECT,
            )
        }

        val replacement = calculateReplacementStatus(today, items)
        val spare = calculateSpareStatus(items)

        return OverallStatusResponse(
            replacement = replacement,
            spare = spare,
            overall = OverallStatus.of(replacement, spare),
        )
    }

    private fun calculateReplacementStatus(today: LocalDate, items: List<ItemSnapshot>): ItemStatus {
        val dangerRatio = items.count { it.isReplacementOverdue(today) }.toDouble() / items.size
        val average = items.sumOf { it.replacementScore(today) }.toDouble() / items.size

        return when {
            dangerRatio == 0.0 -> ItemStatus.GOOD
            dangerRatio <= DANGER_RATIO_WARNING_LIMIT && average >= REPLACEMENT_AVERAGE_WARNING_MIN -> {
                ItemStatus.WARNING
            }
            else -> ItemStatus.DANGER
        }
    }

    private fun calculateSpareStatus(items: List<ItemSnapshot>): ItemStatus {
        val missingRatio = items.count { it.isSpareMissing() }.toDouble() / items.size

        return when {
            missingRatio == 0.0 -> ItemStatus.GOOD
            missingRatio <= DANGER_RATIO_WARNING_LIMIT -> ItemStatus.WARNING
            else -> ItemStatus.DANGER
        }
    }

    private fun calculateMyStatusSummary(today: LocalDate, items: List<ItemSnapshot>): MyStatusSummaryResponse {
        if (items.isEmpty()) {
            return MyStatusSummaryResponse(
                totalCount = 0,
                needReplaceCount = 0,
                score = DEFAULT_AVERAGE_SCORE,
                averageScore = DEFAULT_AVERAGE_SCORE,
            )
        }

        val replacementBar = items.sumOf { it.replacementScore(today) }.toDouble() / items.size / MAX_SCORE * 100
        val spareBar = items.sumOf { it.spareScore() }.toDouble() / items.size / MAX_SCORE * 100

        return MyStatusSummaryResponse(
            totalCount = items.size,
            // 교체 시기가 지난 것만 개수 측정
            needReplaceCount = items.count { it.isReplacementOverdue(today) },
            score = replacementBar * REPLACEMENT_SCORE_WEIGHT + spareBar * SPARE_SCORE_WEIGHT,
            averageScore = DEFAULT_AVERAGE_SCORE,
        )
    }

    private fun bucketize(today: LocalDate, items: List<ItemSnapshot>): List<ItemBucketResponse> = ItemBucket.entries
        .map { bucket ->
            val bucketItems = items.filter { ItemBucket.of(it.spareBand(), it.replacementBand(today)) == bucket }
            ItemBucketResponse(
                bucket = bucket,
                count = bucketItems.size,
                items = bucketItems.map { it.toBucketItemResponse(bucket.status) },
            )
        }

    private fun ItemSnapshot.toBucketItemResponse(status: ItemStatus): BucketItemResponse = BucketItemResponse(
        id = id,
        name = name,
        count = quantity,
        nextReplacementDate = nextReplacementDate,
        status = status,
    )
}
