package depromeet.hotsix.obrit.home.service

import depromeet.hotsix.obrit.home.dto.HomeBucketsResponse
import depromeet.hotsix.obrit.home.dto.MyStatusSummaryResponse
import depromeet.hotsix.obrit.home.dto.OverallStatusResponse
import depromeet.hotsix.obrit.item.service.ItemService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
class HomeService(
    private val itemService: ItemService,
    private val homeStatusCalculatorService: HomeStatusCalculatorService,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun getOverallStatus(userId: Long): OverallStatusResponse {
        val today = LocalDate.now(clock)
        val items = itemService.findActiveSnapshotsByUserId(userId)

        return homeStatusCalculatorService.calculateOverallStatus(today, items)
    }

    @Transactional(readOnly = true)
    fun getMyStatusSummary(userId: Long): MyStatusSummaryResponse {
        val today = LocalDate.now(clock)
        val items = itemService.findActiveSnapshotsByUserId(userId)

        // TODO : 현재 평균 점수는 45점으로 고정된 상황, 추후 유저 평균 점수 계산 로직을 만들어 적용하기
        return homeStatusCalculatorService.calculateMyStatusSummary(today, items)
    }

    fun getBuckets(userId: Long): HomeBucketsResponse {
        val today = LocalDate.now(clock)
        val items = itemQueryService.findActiveSnapshotsByUserId(userId)

        return homeStatusCalculatorService.calculateBuckets(today, items)
    }
}
