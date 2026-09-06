package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.item.entity.ItemNotificationSnapshot
import depromeet.hotsix.obrit.item.service.ItemService
import depromeet.hotsix.obrit.notification.entity.NotificationCandidate
import depromeet.hotsix.obrit.notification.entity.NotificationSettings
import depromeet.hotsix.obrit.notification.entity.NotificationType
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 사전/지연/여분부족 알림 판정 로직.
 *
 * 우선순위: 지연 > 여분 부족 > 사전. 여분 부족이 사전보다 앞서는 것은 여분이 없다는 정보가 더 구체적인 행동을 지시하기 때문이다.
 * 지연 알림은 설정된 스텝 수만큼만 발송하고, 여분 부족 알림은 재입고 전까지 한 번만 발송한다.
 * 교체일이 지났지만 그날이 지연 스텝이 아니면 여분 부족으로 내려간다. 가장 급한 상태가 가장 조용해지면 안 되기 때문이다.
 *
 * 선행 일수와 지연 스텝은 [NotificationSettings]에서 읽는다. 판정 구조 자체는 설정으로 바꿀 수 없다.
 */
@Service
class NotificationPolicyService(
    private val itemService: ItemService,
    private val notificationSettingsService: NotificationSettingsService,
    private val clock: Clock,
) {
    fun evaluate(): List<NotificationCandidate> {
        val today = LocalDate.now(clock)
        val settings = notificationSettingsService.current()
        val leadDays = settings.leadDays
        val overdueSteps = settings.overdueSteps()

        return itemService.findActiveNotificationSnapshots()
            .mapNotNull { evaluate(it, today, leadDays, overdueSteps) }
            .filter { settings.isEnabled(it.type) }
    }

    private fun evaluate(
        item: ItemNotificationSnapshot,
        today: LocalDate,
        leadDays: Int,
        overdueSteps: List<Int>,
    ): NotificationCandidate? {
        val daysUntil = ChronoUnit.DAYS.between(today, item.nextReplacementDate).toInt()

        val isLowStock = item.quantity == 0 && daysUntil <= leadDays && item.lowStockNotifiedAt == null
        val type = when {
            // 지연 스텝에 걸리지 않는 날이라도 여분이 없으면 알려야 한다.
            // 폴백이 없으면 스텝을 소진했거나 스텝 사이에 낀 소모품이 여분 0인 채로 조용해진다.
            daysUntil < 0 -> overdueType(item, daysUntil, overdueSteps)
                ?: NotificationType.LOW_STOCK.takeIf { isLowStock }
            isLowStock -> NotificationType.LOW_STOCK
            daysUntil == leadDays -> NotificationType.PRE_REPLACEMENT
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

    private fun overdueType(
        item: ItemNotificationSnapshot,
        daysUntil: Int,
        overdueSteps: List<Int>,
    ): NotificationType? {
        val stepIndex = item.overdueNotifiedCount
        if (stepIndex >= overdueSteps.size) return null

        val daysOverdue = -daysUntil
        return if (daysOverdue == overdueSteps[stepIndex]) NotificationType.OVERDUE else null
    }
}
