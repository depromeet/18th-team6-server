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
 *
 * 상위 유형이 성립하지 않거나 꺼져 있으면 그 아래 유형으로 내려간다. 교체일이 지났는데 그날이 지연 스텝이
 * 아니면 여분 부족으로, 여분 부족이 꺼져 있으면 사전으로 내려가는 식이다. 가장 급한 상태가 가장 조용해지면 안 된다.
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
        val overdueSteps = settings.overdueSteps()

        return itemService.findActiveNotificationSnapshots()
            .mapNotNull { evaluate(it, today, settings, overdueSteps) }
    }

    private fun evaluate(
        item: ItemNotificationSnapshot,
        today: LocalDate,
        settings: NotificationSettings,
        overdueSteps: List<Int>,
    ): NotificationCandidate? {
        val daysUntil = ChronoUnit.DAYS.between(today, item.nextReplacementDate).toInt()
        val type = applicableTypes(item, daysUntil, settings.leadDays, overdueSteps)
            .firstOrNull { settings.isEnabled(it) }
            ?: return null

        return NotificationCandidate(
            itemId = item.id,
            userId = item.userId,
            itemName = item.name,
            type = type,
            daysUntil = daysUntil,
        )
    }

    /**
     * 이 소모품에 성립하는 알림 유형을 우선순위 순으로 돌려준다. 성립하지 않으면 비어 있다.
     *
     * 유형별 on/off는 호출부가 이 목록을 훑으며 적용한다. 판정을 먼저 끝내고 나중에 거르면
     * 상위 유형이 꺼져 있을 때 아래 유형까지 같이 사라진다.
     */
    private fun applicableTypes(
        item: ItemNotificationSnapshot,
        daysUntil: Int,
        leadDays: Int,
        overdueSteps: List<Int>,
    ): List<NotificationType> {
        val isLowStock = item.quantity == 0 && daysUntil <= leadDays && item.lowStockNotifiedAt == null

        return buildList {
            if (daysUntil < 0 && isOverdueStep(item, daysUntil, overdueSteps)) add(NotificationType.OVERDUE)
            if (isLowStock) add(NotificationType.LOW_STOCK)
            if (daysUntil == leadDays) add(NotificationType.PRE_REPLACEMENT)
        }
    }

    /** 오늘이 이 소모품의 다음 지연 알림 스텝인지. 스텝을 모두 소진했으면 false. */
    private fun isOverdueStep(item: ItemNotificationSnapshot, daysUntil: Int, overdueSteps: List<Int>): Boolean {
        val stepIndex = item.overdueNotifiedCount
        if (stepIndex >= overdueSteps.size) return false

        return -daysUntil == overdueSteps[stepIndex]
    }
}
