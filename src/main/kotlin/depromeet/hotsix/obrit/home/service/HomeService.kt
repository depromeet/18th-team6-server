package depromeet.hotsix.obrit.home.service

import depromeet.hotsix.obrit.home.dto.MyStatusSummaryResponse
import depromeet.hotsix.obrit.home.dto.OverallStatusResponse
import depromeet.hotsix.obrit.item.service.ItemQueryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

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

        //TODO : 현재 평균 점수는 45점으로 고정된 상황, 추후 유저 평균 점수 계산 로직을 만들어 적용하기
        return homeStatusCalculatorService.calculateMyStatusSummary(today, items)
    }
}
