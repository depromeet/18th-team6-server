package depromeet.hotsix.obrit.home.service

import depromeet.hotsix.obrit.global.readmodel.ItemSnapshot
import depromeet.hotsix.obrit.home.dto.BucketItemResponse
import depromeet.hotsix.obrit.home.dto.HomeResponse
import depromeet.hotsix.obrit.home.dto.ItemBucket
import depromeet.hotsix.obrit.home.dto.ItemBucketResponse
import depromeet.hotsix.obrit.home.dto.ItemStatus
import depromeet.hotsix.obrit.home.dto.MyStatusSummaryResponse
import depromeet.hotsix.obrit.home.dto.OverallStatus
import depromeet.hotsix.obrit.home.dto.OverallStatusResponse
import depromeet.hotsix.obrit.home.dto.ReplacementBand
import depromeet.hotsix.obrit.home.dto.SpareBand
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class HomeStatusCalculatorService {

    companion object {
        private const val SCORE_DANGER = 0
        private const val SCORE_WARNING = 1
        private const val SCORE_GOOD = 2
        private const val MAX_SCORE = 2.0
        private const val SPARE_WARNING_MIN = 1
        private const val SPARE_GOOD_MIN = 3
        private const val REPLACEMENT_WARN_DAYS = 3
        private const val DANGER_RATIO_WARNING_LIMIT = 0.3
        private const val REPLACEMENT_AVERAGE_WARNING_MIN = 1.0
        private const val EMPTY_SCORE = 45.0
        private const val REPLACEMENT_SCORE_WEIGHT = 0.6
        private const val SPARE_SCORE_WEIGHT = 0.4
    }

    fun calculate(today: LocalDate, items: List<ItemSnapshot>): HomeResponse = HomeResponse(
        overallStatus = calculateOverallStatus(today, items),
        myStatusSummary = calculateMyStatusSummary(today, items),
        itemBuckets = bucketize(today, items),
    )

    private fun calculateOverallStatus(today: LocalDate, items: List<ItemSnapshot>): OverallStatusResponse {
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
            overall = combineOverallStatus(replacement, spare),
        )
    }

    private fun calculateReplacementStatus(today: LocalDate, items: List<ItemSnapshot>): ItemStatus {
        val dangerCount = items.count { replacementScore(today, it) == SCORE_DANGER }
        val dangerRatio = dangerCount.toDouble() / items.size
        val average = items.sumOf { replacementScore(today, it) }.toDouble() / items.size

        return when {
            dangerRatio == 0.0 -> ItemStatus.GOOD
            dangerRatio <= DANGER_RATIO_WARNING_LIMIT && average >= REPLACEMENT_AVERAGE_WARNING_MIN -> {
                ItemStatus.WARNING
            }
            else -> ItemStatus.DANGER
        }
    }

    private fun calculateSpareStatus(items: List<ItemSnapshot>): ItemStatus {
        val missingRatio = items.count { it.quantity == 0 }.toDouble() / items.size

        return when {
            missingRatio == 0.0 -> ItemStatus.GOOD
            missingRatio <= DANGER_RATIO_WARNING_LIMIT -> ItemStatus.WARNING
            else -> ItemStatus.DANGER
        }
    }

    private fun combineOverallStatus(replacement: ItemStatus, spare: ItemStatus): OverallStatus {
        if (replacement == ItemStatus.GOOD && spare == ItemStatus.GOOD) {
            return OverallStatus.PERFECT
        }
        if (replacement == ItemStatus.GOOD && spare == ItemStatus.WARNING) {
            return OverallStatus.GOOD
        }
        if (replacement == ItemStatus.WARNING && spare == ItemStatus.GOOD) {
            return OverallStatus.GOOD
        }
        if (replacement == ItemStatus.GOOD && spare == ItemStatus.DANGER) {
            return OverallStatus.WARNING
        }
        if (replacement == ItemStatus.DANGER && spare == ItemStatus.GOOD) {
            return OverallStatus.WARNING
        }
        if (replacement == ItemStatus.WARNING && spare == ItemStatus.WARNING) {
            return OverallStatus.WARNING
        }

        return OverallStatus.DANGER
    }

    private fun calculateMyStatusSummary(today: LocalDate, items: List<ItemSnapshot>): MyStatusSummaryResponse {
        if (items.isEmpty()) {
            return MyStatusSummaryResponse(
                totalCount = 0,
                needReplaceCount = 0,
                score = EMPTY_SCORE,
            )
        }

        val replacementBar = items.sumOf { replacementScore(today, it) }.toDouble() / items.size / MAX_SCORE * 100
        val spareBar = items.sumOf { spareScore(it) }.toDouble() / items.size / MAX_SCORE * 100

        return MyStatusSummaryResponse(
            totalCount = items.size,
            needReplaceCount = items.count { replacementBand(today, it) == ReplacementBand.OVERDUE },
            score = replacementBar * REPLACEMENT_SCORE_WEIGHT + spareBar * SPARE_SCORE_WEIGHT,
        )
    }

    private fun bucketize(today: LocalDate, items: List<ItemSnapshot>): List<ItemBucketResponse> = ItemBucket.entries
        .map { bucket ->
            val bucketItems = items.filter { bucketOf(today, it) == bucket }
            ItemBucketResponse(
                bucket = bucket,
                count = bucketItems.size,
                items = bucketItems.map { it.toBucketItemResponse(bucket.status) },
            )
        }

    private fun bucketOf(today: LocalDate, item: ItemSnapshot): ItemBucket {
        val spare = spareBand(item)
        val replacement = replacementBand(today, item)

        if (spare == SpareBand.NONE && replacement == ReplacementBand.OVERDUE) {
            return ItemBucket.NONE_OVERDUE
        }
        if (spare == SpareBand.NONE && replacement == ReplacementBand.WARN) {
            return ItemBucket.NONE_WARN
        }
        if (spare == SpareBand.NONE && replacement == ReplacementBand.SAFE) {
            return ItemBucket.NONE_SAFE
        }
        if (spare == SpareBand.HAS && replacement == ReplacementBand.OVERDUE) {
            return ItemBucket.HAS_OVERDUE
        }
        if (spare == SpareBand.HAS && replacement == ReplacementBand.WARN) {
            return ItemBucket.HAS_WARN
        }

        return ItemBucket.HAS_SAFE
    }

    private fun replacementScore(today: LocalDate, item: ItemSnapshot): Int {
        val band = replacementBand(today, item)

        if (band == ReplacementBand.OVERDUE) {
            return SCORE_DANGER
        }
        if (band == ReplacementBand.WARN) {
            return SCORE_WARNING
        }

        return SCORE_GOOD
    }

    private fun spareScore(item: ItemSnapshot): Int = when {
        item.quantity >= SPARE_GOOD_MIN -> SCORE_GOOD
        item.quantity >= SPARE_WARNING_MIN -> SCORE_WARNING
        else -> SCORE_DANGER
    }

    private fun replacementBand(today: LocalDate, item: ItemSnapshot): ReplacementBand {
        val daysUntil = ChronoUnit.DAYS.between(today, item.nextReplacementDate)

        return when {
            daysUntil <= 0 -> ReplacementBand.OVERDUE
            daysUntil <= REPLACEMENT_WARN_DAYS -> ReplacementBand.WARN
            else -> ReplacementBand.SAFE
        }
    }

    private fun spareBand(item: ItemSnapshot): SpareBand = if (item.quantity == 0) {
        SpareBand.NONE
    } else {
        SpareBand.HAS
    }

    private fun ItemSnapshot.toBucketItemResponse(status: ItemStatus): BucketItemResponse = BucketItemResponse(
        id = id,
        name = name,
        count = quantity,
        nextReplacementDate = nextReplacementDate,
        status = status,
    )
}
