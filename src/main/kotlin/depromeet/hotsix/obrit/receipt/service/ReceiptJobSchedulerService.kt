package depromeet.hotsix.obrit.receipt.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * 대기중 영수증 잡을 주기적으로 폴링해 처리하는 스케줄 어댑터.
 * 실제 처리 로직은 [ReceiptJobService.processNextPending]에 위임한다.
 */
@Service
class ReceiptJobSchedulerService(private val receiptJobService: ReceiptJobService) {

    @Scheduled(fixedDelay = POLL_INTERVAL_MS)
    fun poll() {
        receiptJobService.processNextPending()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 500L
    }
}
