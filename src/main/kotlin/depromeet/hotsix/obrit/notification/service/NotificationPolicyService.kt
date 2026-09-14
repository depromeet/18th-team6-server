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
 * 사전/지연/여분부족 알림 판정 로직.
 *
 * 우선순위: 지연 > 여분 부족 > 사전. 여분 부족이 사전보다 앞서는 것은 여분이 없다는 정보가 더 구체적인 행동을 지시하기 때문이다.
 * 지연 알림은 설정된 스텝 수만큼만 발송하고, 여분 부족 알림은 재입고 전까지 한 번만 발송한다.
 *
 * 상위 유형이 성립하지 않거나 꺼져 있으면 그 아래 유형으로 내려간다. 교체일이 지났는데 그날이 지연 스텝이
 * 아니면 여분 부족으로, 여분 부족이 꺼져 있으면 사전으로 내려가는 식이다. 가장 급한 상태가 가장 조용해지면 안 된다.
 *
 * 선행 일수는 유저 설정, 지연 스텝은 전역 설정에서 읽는다. 유형별 발송 여부는
 * `전역 유형 활성 AND 유저 전체 수신 AND 유저 유형 활성`이며, 전역은 킬 스위치로 동작한다.
 * 판정 구조 자체는 설정으로 바꿀 수 없다.
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
        val overdueSteps = global.overdueSteps()
        val snapshots = itemService.findActiveNotificationSnapshots()
        val userSettings = userNotificationSettingsService
            .effectiveSettingsByUserIds(snapshots.mapTo(mutableSetOf()) { it.userId })

        return snapshots.mapNotNull {
            evaluate(it, today, global, userSettings.getValue(it.userId), overdueSteps)
        }
    }

    private fun evaluate(
        item: ItemNotificationSnapshot,
        today: LocalDate,
        global: NotificationSettings,
        user: EffectiveNotificationSettings,
        overdueSteps: List<Int>,
    ): NotificationCandidate? {
        val daysUntil = ChronoUnit.DAYS.between(today, item.nextReplacementDate).toInt()
        val type = applicableTypes(item, today, daysUntil, user.leadDays, overdueSteps)
            .firstOrNull { global.isEnabled(it) && user.isEnabled(it) }
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
        today: LocalDate,
        daysUntil: Int,
        leadDays: Int,
        overdueSteps: List<Int>,
    ): List<NotificationType> {
        // 미입력(null)은 대상이 아니다. 0은 사용자가 "여분 없음"을 명시한 값이라 대상이 된다.
        val isLowStock = item.quantity == 0 && daysUntil <= leadDays && item.lowStockNotifiedAt == null

        return buildList {
            if (daysUntil < 0 && isOverdueStepDue(item, today, daysUntil, overdueSteps)) {
                add(NotificationType.OVERDUE)
            }
            if (isLowStock) add(NotificationType.LOW_STOCK)
            if (daysUntil == leadDays) add(NotificationType.PRE_REPLACEMENT)
        }
    }

    /**
     * 다음 지연 알림 스텝에 도달했는지. 스텝을 모두 소진했으면 false.
     *
     * 정확 일치가 아니라 경과 기준(`>=`)으로 본다. 일치로 보면 그 하루를 놓친 아이템이 영원히 복구되지 않는다.
     * 이미 D+7을 넘긴 아이템은 다음 스텝이 D+1인데 경과일이 8, 20, 100이라 일치할 수 없고,
     * 스케줄러가 하루만 걸러도 그날 스텝이 영구 유실된다. 가장 오래 방치된 소모품이 가장 확실하게 빠지는 셈이다.
     *
     * 대신 스텝 간 최소 간격을 둔다. `>=`만 적용하면 D+8 아이템이 D+1·D+4·D+7 스텝을 날마다 연달아 소진한다.
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
