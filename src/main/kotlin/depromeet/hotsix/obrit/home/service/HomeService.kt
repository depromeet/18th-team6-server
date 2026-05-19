package depromeet.hotsix.obrit.home.service

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
}
