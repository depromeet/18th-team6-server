package depromeet.hotsix.obrit.home.service

import depromeet.hotsix.obrit.global.paging.CursorSliceResponse
import depromeet.hotsix.obrit.global.paging.normalizePageSize
import depromeet.hotsix.obrit.global.readmodel.ItemListSnapshot
import depromeet.hotsix.obrit.global.readmodel.ItemOrder
import depromeet.hotsix.obrit.home.dto.HomeBucketsResponse
import depromeet.hotsix.obrit.home.dto.HomeItemCard
import depromeet.hotsix.obrit.home.dto.MyStatusSummaryResponse
import depromeet.hotsix.obrit.home.dto.OverallStatusResponse
import depromeet.hotsix.obrit.item.service.ItemQueryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@Service
@Transactional(readOnly = true)
class HomeService(
    private val itemQueryService: ItemQueryService,
    private val homeStatusCalculatorService: HomeStatusCalculatorService,
    private val clock: Clock,
) {

    fun getOverallStatus(userId: Long): OverallStatusResponse {
        val today = LocalDate.now(clock)
        val items = itemQueryService.findActiveSnapshotsByUserId(userId)

        return homeStatusCalculatorService.calculateOverallStatus(today, items)
    }

    fun getMyStatusSummary(userId: Long): MyStatusSummaryResponse {
        val today = LocalDate.now(clock)
        val items = itemQueryService.findActiveSnapshotsByUserId(userId)

        // TODO : 현재 평균 점수는 45점으로 고정된 상황, 추후 유저 평균 점수 계산 로직을 만들어 적용하기
        return homeStatusCalculatorService.calculateMyStatusSummary(today, items)
    }

    fun getBuckets(userId: Long): HomeBucketsResponse {
        val today = LocalDate.now(clock)
        val items = itemQueryService.findActiveSnapshotsByUserId(userId)

        return homeStatusCalculatorService.calculateBuckets(today, items)
    }

    fun getItems(
        userId: Long,
        order: ItemOrder,
        dDay: Int?,
        spareQuantity: Int?,
        cursor: Long?,
        size: Int,
    ): CursorSliceResponse<HomeItemCard> {
        val today = LocalDate.now(clock)
        val pageSize = normalizePageSize(size)
        val items = itemQueryService.findItemListSnapshots(
            userId = userId,
            order = order,
            dDay = dDay,
            spareQuantity = spareQuantity,
            cursor = cursor,
            today = today,
            size = pageSize + 1,
        )
        return CursorSliceResponse.fromFetched(
            fetchedContent = items.map { it.toHomeItemCard(today) },
            size = pageSize,
            cursorSelector = { it.id },
        )
    }

    private fun ItemListSnapshot.toHomeItemCard(today: LocalDate): HomeItemCard = HomeItemCard(
        id = id,
        name = name,
        daysInUse = ChronoUnit.DAYS.between(lastReplacedDate, today).toInt().coerceAtLeast(0),
        replacementDday = replacementLabel(ChronoUnit.DAYS.between(today, nextReplacementDate)),
        spareQuantity = quantity,
    )

    private fun replacementLabel(daysUntil: Long): String = when {
        daysUntil == 0L -> "교체 D-day"
        daysUntil > 0 -> "교체 D-$daysUntil"
        else -> "교체 D+${abs(daysUntil)}"
    }
}
