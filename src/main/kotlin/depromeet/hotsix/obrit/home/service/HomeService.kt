package depromeet.hotsix.obrit.home.service

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
}
