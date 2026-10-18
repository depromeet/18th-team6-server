package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.item.entity.ItemNotificationSnapshot
import depromeet.hotsix.obrit.item.service.ItemService
import depromeet.hotsix.obrit.notification.entity.EffectiveNotificationSettings
import depromeet.hotsix.obrit.notification.entity.NotificationCandidate
import depromeet.hotsix.obrit.notification.entity.NotificationSettings
import depromeet.hotsix.obrit.notification.entity.NotificationType
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 사전·당일/지연/여분 부족 알림 판정 로직.
 *
 * 일정 사유는 사전·당일(사전 스텝, 예: D-7·D-3·D-1·D-day)과 지연(지연 스텝, 예: D+1·D+3·D+7) 중 하나다. 둘은 날짜상 겹치지 않는다.
 * 여분 부족은 일정과 따로 판정해, 같은 날 함께 성립하면 하나를 고르지 않고 한 후보의 사유로 합친다.
 * 지연 알림은 설정된 스텝 수만큼만 발송하고, 여분 부족 알림은 재입고 전까지 한 번만 발송한다.
 * 사전·당일 알림은 상태를 저장하지 않고 스텝과 정확히 일치하는 날에만 보낸다.
 *
 * 각 사유는 `전역 유형 활성 AND 유저 유형 활성`으로 따로 거른다. 전역은 킬 스위치로 동작한다.
 * 꺼진 사유만 빠지고 남은 사유가 없으면 후보가 아니다.
 */
@Service
class NotificationPolicyService(
    private val itemService: ItemService,
    private val notificationSettingsService: NotificationSettingsService,
    private val userNotificationSettingsService: UserNotificationSettingsService,
    private val clock: Clock,
) {
    fun evaluate(): List<NotificationCandidate> {
        val today = LocalDate.now(clock)
        val global = notificationSettingsService.current()
        val preReplacementSteps = global.preReplacementSteps().toSet()
        val overdueSteps = global.overdueSteps()
        val snapshots = itemService.findActiveNotificationSnapshots()
        val userSettings = userNotificationSettingsService
            .effectiveSettingsByUserIds(snapshots.mapTo(mutableSetOf()) { it.userId })

        return snapshots.mapNotNull {
            evaluate(it, today, global, userSettings.getValue(it.userId), preReplacementSteps, overdueSteps)
        }
    }

    private fun evaluate(
        item: ItemNotificationSnapshot,
        today: LocalDate,
        global: NotificationSettings,
        user: EffectiveNotificationSettings,
        preReplacementSteps: Set<Int>,
        overdueSteps: List<Int>,
    ): NotificationCandidate? {
        val daysUntil = ChronoUnit.DAYS.between(today, item.nextReplacementDate).toInt()
        val reasons = listOfNotNull(
            scheduleReason(item, today, daysUntil, preReplacementSteps, overdueSteps),
            lowStockReason(item, daysUntil, global.lowStockLeadDays),
        ).filter { global.isEnabled(it) && user.isEnabled(it) }
        if (reasons.isEmpty()) return null

        return NotificationCandidate(
            itemId = item.id,
            userId = item.userId,
            itemName = item.name,
            reasons = reasons,
            daysUntil = daysUntil,
            nextReplacementDate = item.nextReplacementDate,
        )
    }

    /** 오늘 성립하는 일정 사유. 교체일 전·당일은 사전 스텝, 지난 뒤는 지연 스텝으로 본다. */
    private fun scheduleReason(
        item: ItemNotificationSnapshot,
        today: LocalDate,
        daysUntil: Int,
        preReplacementSteps: Set<Int>,
        overdueSteps: List<Int>,
    ): NotificationType? = when {
        daysUntil < 0 -> NotificationType.OVERDUE.takeIf { isOverdueStepDue(item, today, daysUntil, overdueSteps) }
        daysUntil in preReplacementSteps -> NotificationType.PRE_REPLACEMENT
        else -> null
    }

    /** 미입력(null)은 대상이 아니다. 0은 사용자가 "여분 없음"을 명시한 값이라 대상이 된다. */
    private fun lowStockReason(item: ItemNotificationSnapshot, daysUntil: Int, leadDays: Int): NotificationType? =
        NotificationType.LOW_STOCK.takeIf {
            item.quantity == 0 && daysUntil <= leadDays && item.lowStockNotifiedAt == null
        }

    /**
     * 다음 지연 알림 스텝에 도달했는지. 스텝을 모두 소진했으면 false.
     *
     * 정확 일치가 아니라 경과 기준(`>=`)으로 본다. 일치로 보면 그 하루를 놓친 아이템이 영원히 복구되지 않는다.
     * 이미 D+7을 넘긴 아이템은 다음 스텝이 D+1인데 경과일이 8, 20, 100이라 일치할 수 없고,
     * 스케줄러가 하루만 걸러도 그날 스텝이 영구 유실된다. 가장 오래 방치된 소모품이 가장 확실하게 빠지는 셈이다.
     *
     * 대신 스텝 간 최소 간격을 둔다. `>=`만 적용하면 D+8 아이템이 D+1·D+3·D+7 스텝을 날마다 연달아 소진한다.
     */
    private fun isOverdueStepDue(
        item: ItemNotificationSnapshot,
        today: LocalDate,
        daysUntil: Int,
        overdueSteps: List<Int>,
    ): Boolean {
        val stepIndex = item.overdueNotifiedCount
        if (stepIndex >= overdueSteps.size) return false
        if (-daysUntil < overdueSteps[stepIndex]) return false

        return hasWaitedMinInterval(item, today, stepIndex, overdueSteps)
    }

    /** 첫 스텝은 대기 없이 보낸다. 이후 스텝은 원래 스텝 간격만큼 지난 뒤에만 보낸다. */
    private fun hasWaitedMinInterval(
        item: ItemNotificationSnapshot,
        today: LocalDate,
        stepIndex: Int,
        overdueSteps: List<Int>,
    ): Boolean {
        val lastNotifiedAt = item.lastOverdueNotifiedAt ?: return true
        if (stepIndex == 0) return true

        val minInterval = overdueSteps[stepIndex] - overdueSteps[stepIndex - 1]

        return ChronoUnit.DAYS.between(lastNotifiedAt, today) >= minInterval
    }
}
