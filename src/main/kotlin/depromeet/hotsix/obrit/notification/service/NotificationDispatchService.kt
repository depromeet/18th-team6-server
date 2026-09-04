package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.item.service.ItemService
import depromeet.hotsix.obrit.notification.entity.Notification
import depromeet.hotsix.obrit.notification.entity.NotificationCandidate
import depromeet.hotsix.obrit.notification.entity.NotificationType
import depromeet.hotsix.obrit.notification.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDate

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

    fun dispatch() {
        val today = LocalDate.now(clock)
        val candidatesByUser = notificationPolicyService.evaluate().groupBy { it.userId }
        log.info("알림 배치 시작. 대상 유저 수={}", candidatesByUser.size)
        candidatesByUser.forEach { (userId, candidates) -> dispatchToUser(userId, candidates, today) }
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
}
