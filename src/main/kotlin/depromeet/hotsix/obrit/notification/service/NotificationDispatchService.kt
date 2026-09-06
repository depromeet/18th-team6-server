package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.item.service.ItemService
import depromeet.hotsix.obrit.notification.entity.Notification
import depromeet.hotsix.obrit.notification.entity.NotificationCandidate
import depromeet.hotsix.obrit.notification.entity.NotificationPreviewSnapshot
import depromeet.hotsix.obrit.notification.entity.NotificationType
import depromeet.hotsix.obrit.notification.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.locks.ReentrantLock

/**
 * 알림 판정 결과를 유저 단위로 묶어 발송한다.
 *
 * 같은 배치 실행에서 유저당 대상이 둘 이상이면 묶음 알림 1건만 발송한다(7.3).
 * TransactionTemplate으로 경계를 직접 관리하는 이유는 [[DeviceRegistrationService]]와 동일하게,
 * 같은 클래스 내 self-invocation으로는 `@Transactional` 프록시가 적용되지 않기 때문이다.
 */
@Service
class NotificationDispatchService(
    private val notificationPolicyService: NotificationPolicyService,
    private val notificationRepository: NotificationRepository,
    private val fcmPushService: FcmPushService,
    private val itemService: ItemService,
    private val clock: Clock,
    transactionManager: PlatformTransactionManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val transaction = TransactionTemplate(transactionManager)

    /**
     * 단일 인스턴스 기준으로 배치 중복 실행을 막는다.
     * 인스턴스를 늘리면 DB 잠금 등 클러스터 단위 수단으로 바꿔야 한다.
     */
    private val dispatchLock = ReentrantLock()

    /**
     * 실제 발송한 사용자 수를 반환한다.
     *
     * 배치가 겹쳐 실행되면 같은 후보를 두 번 계산해 중복 발송되고
     * 지연 알림 단계가 한 번에 두 칸 넘어간다. 그래서 동시 실행을 막는다.
     */
    fun dispatch(): Int {
        if (!dispatchLock.tryLock()) {
            throw BusinessException("알림 배치가 이미 실행 중입니다. 잠시 후 다시 시도해주세요.")
        }

        return try {
            runDispatch()
        } finally {
            dispatchLock.unlock()
        }
    }

    private fun runDispatch(): Int {
        val today = LocalDate.now(clock)
        val candidatesByUser = notificationPolicyService.evaluate().groupBy { it.userId }
        log.info("알림 배치 시작. 대상 유저 수={}", candidatesByUser.size)

        var sent = 0
        var failed = 0
        candidatesByUser.forEach { (userId, candidates) ->
            // 한 사용자의 실패가 남은 사용자의 발송까지 막지 않도록 격리한다.
            runCatching { dispatchToUser(userId, candidates, today) }
                .onSuccess { sent++ }
                .onFailure {
                    failed++
                    log.error("알림 발송 실패. userId={}", userId, it)
                }
        }

        log.info("알림 배치 종료. 발송={}, 실패={}", sent, failed)
        return sent
    }

    /**
     * 발송하지 않고 대상만 집계한다.
     *
     * 260명 규모에 한 번에 나가는 발송이라 규모를 눈으로 확인한 뒤 실행할 수 있어야 한다.
     */
    fun preview(): NotificationPreviewSnapshot {
        val candidatesByUser = notificationPolicyService.evaluate().groupBy { it.userId }

        return NotificationPreviewSnapshot(
            targetUserCount = candidatesByUser.size,
            targetItemCount = candidatesByUser.values.sumOf { it.size },
            bundledUserCount = candidatesByUser.count { (_, candidates) -> candidates.size > 1 },
            countByType = candidatesByUser.values.flatten()
                .groupingBy { it.type }
                .eachCount(),
            samples = candidatesByUser.entries
                .sortedBy { (_, candidates) -> candidates.minOf { it.daysUntil } }
                .take(SAMPLE_SIZE)
                .map { (userId, candidates) ->
                    val sorted = candidates.sortedBy { it.daysUntil }
                    val message = buildMessage(sorted)
                    NotificationPreviewSnapshot.Sample(
                        userId = userId,
                        itemCount = sorted.size,
                        title = message.title,
                        body = message.body,
                    )
                },
        )
    }

    private fun dispatchToUser(userId: Long, candidates: List<NotificationCandidate>, today: LocalDate) {
        val sorted = candidates.sortedBy { it.daysUntil }
        val message = buildMessage(sorted)

        transaction.executeWithoutResult {
            notificationRepository.save(
                Notification(userId = userId, type = sorted.first().type, title = message.title, body = message.body),
            )
            sorted.forEach { recordSent(it, today) }
        }

        fcmPushService.sendToUser(userId, message.title, message.body)
    }

    private fun recordSent(candidate: NotificationCandidate, today: LocalDate) {
        when (candidate.type) {
            NotificationType.OVERDUE -> itemService.recordOverdueNotification(candidate.itemId, today)
            NotificationType.LOW_STOCK -> itemService.recordLowStockNotification(candidate.itemId, today)
            NotificationType.PRE_REPLACEMENT -> Unit
            NotificationType.NOTICE -> error("공지는 정책 판정 대상이 아니다.")
        }
    }

    private fun buildMessage(sorted: List<NotificationCandidate>): NotificationMessage =
        if (sorted.size == 1) singleMessage(sorted.single()) else bundleMessage(sorted)

    private fun singleMessage(candidate: NotificationCandidate): NotificationMessage = when (candidate.type) {
        NotificationType.PRE_REPLACEMENT -> NotificationMessage(
            title = "교체 시기가 다가와요",
            body = "${candidate.itemName} 교체가 얼마 남지 않았어요",
        )
        NotificationType.OVERDUE -> NotificationMessage(
            title = "교체 시기가 지났어요",
            body = "${candidate.itemName} 교체 예정일이 지났어요",
        )
        NotificationType.LOW_STOCK -> NotificationMessage(
            title = "여분이 부족해요",
            body = "${candidate.itemName} 여분이 없어요. 지금 준비해두세요",
        )
        NotificationType.NOTICE -> error("공지는 정책 판정 대상이 아니다.")
    }

    private fun bundleMessage(sorted: List<NotificationCandidate>): NotificationMessage {
        val mostUrgent = sorted.first()
        val othersCount = sorted.size - 1
        return NotificationMessage(
            title = "교체가 필요한 소모품이 있어요",
            body = "${mostUrgent.itemName} 외 ${othersCount}건, 확인해보세요",
        )
    }

    private data class NotificationMessage(val title: String, val body: String)

    companion object {
        private const val SAMPLE_SIZE = 5
    }
}
