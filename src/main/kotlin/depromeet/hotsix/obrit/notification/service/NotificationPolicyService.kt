package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.item.entity.ItemNotificationSnapshot
import depromeet.hotsix.obrit.item.service.ItemService
import depromeet.hotsix.obrit.notification.entity.NotificationCandidate
import depromeet.hotsix.obrit.notification.entity.NotificationType
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 사전/지연/여분부족 알림 판정 로직.
 *
 * 우선순위: 여분 부족 알림이 사전 알림보다 우선한다(여분이 없다는 정보가 더 구체적인 행동을 지시하므로).
 * 지연 알림은 D+1/D+4/D+7 세 번만 발송하고, 여분 부족 알림은 재입고 전까지 한 번만 발송한다.
 */
@Service
class NotificationPolicyService(private val itemService: ItemService, private val clock: Clock) {
    fun evaluate(): List<NotificationCandidate> {
        val today = LocalDate.now(clock)
        return itemService.findActiveNotificationSnapshots().mapNotNull { evaluate(it, today) }
    }

    private fun evaluate(item: ItemNotificationSnapshot, today: LocalDate): NotificationCandidate? {
        val daysUntil = ChronoUnit.DAYS.between(today, item.nextReplacementDate).toInt()

        val isLowStock = item.quantity == 0 && daysUntil <= LEAD_DAYS && item.lowStockNotifiedAt == null
        val type = when {
            daysUntil < 0 -> overdueType(item, daysUntil)
            isLowStock -> NotificationType.LOW_STOCK
            daysUntil == LEAD_DAYS -> NotificationType.PRE_REPLACEMENT
            else -> null
        } ?: return null

        return NotificationCandidate(
            itemId = item.id,
            userId = item.userId,
            itemName = item.name,
            type = type,
            daysUntil = daysUntil,
        )
    }

    private fun overdueType(item: ItemNotificationSnapshot, daysUntil: Int): NotificationType? {
        val stepIndex = item.overdueNotifiedCount
        if (stepIndex >= OVERDUE_STEPS.size) return null

        val daysOverdue = -daysUntil
        return if (daysOverdue == OVERDUE_STEPS[stepIndex]) NotificationType.OVERDUE else null
    }

    companion object {
        private const val LEAD_DAYS = 3
        private val OVERDUE_STEPS = listOf(1, 4, 7)
    }
}
