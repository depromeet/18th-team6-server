package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.receipt.client.GeminiRateLimiter
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * 대기중 영수증 잡을 주기적으로 폴링해 처리하는 스케줄 어댑터.
 *
 * 레이트리밋 게이트 → 배치 선점(pick) → 토큰 소비 → 배치 처리(process) 순으로 오케스트레이션한다.
 * pick과 process를 별도 트랜잭션으로 호출해 PROCESSING이 외부에서 관측되도록 한다.
 * 배치 하나는 Gemini 호출 1회이므로 토큰도 1개만 소비한다.
 */
@Service
class ReceiptJobSchedulerService(
    private val receiptJobService: ReceiptJobService,
    private val geminiRateLimiter: GeminiRateLimiter,
    @Value("\${receipt.batch.max-size:6}") private val maxBatchSize: Int,
) {

    @Scheduled(fixedDelay = POLL_INTERVAL_MS)
    fun poll() {
        // 1. DB 조회 전 게이트: 호출 가능한 토큰이 없으면 즉시 종료
        if (!geminiRateLimiter.hasAvailableToken()) {
            return
        }

        // 2. 대기중 잡을 배치로 선점 (pick 트랜잭션)
        val jobIds = receiptJobService.pickBatch(maxBatchSize)
        if (jobIds.isEmpty()) {
            return
        }

        // 3. 실제 호출 직전 토큰 소비. 실패 시 선점한 잡들을 대기중으로 되돌림
        if (!geminiRateLimiter.tryConsume()) {
            receiptJobService.releaseToPending(jobIds)
            return
        }

        // 4. 배치 처리 (process 트랜잭션)
        receiptJobService.processBatch(jobIds)
    }

    /** 일정 시간 이상 PROCESSING에 멈춘 잡을 주기적으로 대기중으로 회수한다. */
    @Scheduled(fixedDelay = RECOVERY_INTERVAL_MS)
    fun recoverStuck() {
        receiptJobService.recoverStuckProcessing()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 500L
        private const val RECOVERY_INTERVAL_MS = 60_000L
    }
}
