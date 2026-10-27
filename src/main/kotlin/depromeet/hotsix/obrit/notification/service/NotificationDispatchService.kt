package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.item.service.ItemService
import depromeet.hotsix.obrit.notification.entity.FcmSendOutcome
import depromeet.hotsix.obrit.notification.entity.Notification
import depromeet.hotsix.obrit.notification.entity.NotificationCandidate
import depromeet.hotsix.obrit.notification.entity.NotificationDispatchResult
import depromeet.hotsix.obrit.notification.entity.NotificationEntry
import depromeet.hotsix.obrit.notification.entity.NotificationPreviewSnapshot
import depromeet.hotsix.obrit.notification.entity.NotificationType
import depromeet.hotsix.obrit.notification.repository.NotificationEntryRepository
import depromeet.hotsix.obrit.notification.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
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
    private val notificationEntryRepository: NotificationEntryRepository,
    private val fcmPushService: FcmPushService,
    private val itemService: ItemService,
    private val clock: Clock,
    private val notificationDeepLinkService: NotificationDeepLinkService,
    private val userNotificationSettingsService: UserNotificationSettingsService,
    transactionManager: PlatformTransactionManager,
    @Value("\${notification.schedule.zone:Asia/Seoul}") scheduleZone: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val transaction = TransactionTemplate(transactionManager)
    private val scheduleClock = clock.withZone(ZoneId.of(scheduleZone))

    /**
     * 단일 인스턴스 기준으로 배치 중복 실행을 막는다.
     * 인스턴스를 늘리면 DB 잠금 등 클러스터 단위 수단으로 바꿔야 한다.
     */
    private val dispatchLock = ReentrantLock()

    /**
     * 발송 결과를 사용자 수 기준으로 집계해 반환한다.
     *
     * 배치가 겹쳐 실행되면 같은 후보를 두 번 계산해 중복 발송되고
     * 지연 알림 단계가 한 번에 두 칸 넘어간다. 그래서 동시 실행을 막는다.
     */
    fun dispatch(): NotificationDispatchResult = dispatch(respectDispatchTime = false)

    /** 사용자별 발송 시각이 지난 당일 미발송 사용자만 처리한다. */
    fun dispatchScheduled(): NotificationDispatchResult = dispatch(respectDispatchTime = true)

    private fun dispatch(respectDispatchTime: Boolean): NotificationDispatchResult {
        if (!dispatchLock.tryLock()) {
            throw BusinessException("알림 배치가 이미 실행 중입니다. 잠시 후 다시 시도해주세요.")
        }

        return try {
            runDispatch(respectDispatchTime)
        } finally {
            dispatchLock.unlock()
        }
    }

    private fun runDispatch(respectDispatchTime: Boolean): NotificationDispatchResult {
        val now = ZonedDateTime.now(scheduleClock)
        val today = now.toLocalDate()
        val candidatesByUser = candidatesByUser(today, now.toLocalTime().takeIf { respectDispatchTime })
        log.info("알림 배치 시작. 대상 유저 수={}", candidatesByUser.size)

        var sent = 0
        var failed = 0
        var skipped = 0
        candidatesByUser.forEach { (userId, candidates) ->
            // 한 사용자의 실패가 남은 사용자의 발송까지 막지 않도록 격리한다.
            runCatching { dispatchToUser(userId, candidates, today) }
                .onSuccess {
                    when (it) {
                        FcmSendOutcome.SENT -> sent++
                        FcmSendOutcome.FAILED -> failed++
                        FcmSendOutcome.NO_DEVICE -> skipped++
                    }
                }
                .onFailure {
                    failed++
                    log.error("알림 발송 실패. userId={}", userId, it)
                }
        }

        log.info("알림 배치 종료. 발송={}, 실패={}, 기기없음={}", sent, failed, skipped)
        return NotificationDispatchResult(sentUserCount = sent, failedUserCount = failed, skippedUserCount = skipped)
    }

    /**
     * 발송하지 않고 대상만 집계한다.
     *
     * 수동 실행 기준으로 사용자 시각을 무시하고, 오늘 발송을 완료한 사용자는 제외한다.
     */
    fun preview(): NotificationPreviewSnapshot {
        val candidatesByUser = candidatesByUser(LocalDate.now(scheduleClock))

        return NotificationPreviewSnapshot(
            targetUserCount = candidatesByUser.size,
            targetItemCount = candidatesByUser.values.sumOf { it.size },
            bundledUserCount = candidatesByUser.count { (_, candidates) -> candidates.size > 1 },
            // 결합 알림은 사유마다 세므로 합계가 대상 소모품 수보다 클 수 있다.
            countByType = candidatesByUser.values.flatten()
                .flatMap { it.reasons }
                .groupingBy { it }
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

    private fun candidatesByUser(
        today: LocalDate,
        dispatchTime: LocalTime? = null,
    ): Map<Long, List<NotificationCandidate>> {
        val candidates = notificationPolicyService.evaluate(today).groupBy { it.userId }
        val settings = userNotificationSettingsService.effectiveSettingsByUserIds(candidates.keys)
        return candidates.filter { (userId, _) ->
            val user = settings.getValue(userId)
            user.lastDispatchDate != today && (dispatchTime == null || !dispatchTime.isBefore(user.dispatchTime))
        }
    }

    /**
     * 한 사용자에게 발송하고 결과를 돌려준다.
     *
     * 전송을 먼저 하고 성공한 뒤에만 상태를 확정한다. 순서를 뒤집으면 전송이 실패해도 지연 스텝이
     * 소진되고 여분 부족 기록이 남아 재발송 기회가 사라진다. 특히 여분 부족은 재입고 전까지 복구되지 않는다.
     *
     * 기기가 없는 사용자도 상태는 확정한다. 재시도해도 결과가 같은데 상태를 미루면 매 배치마다 후보로
     * 다시 계산되고, 기기를 등록하는 순간 밀린 알림이 한꺼번에 나간다. 푸시가 닿지 않을 뿐 인앱 알림 목록에는 남는다.
     */
    private fun dispatchToUser(
        userId: Long,
        candidates: List<NotificationCandidate>,
        today: LocalDate,
    ): FcmSendOutcome {
        val sorted = candidates.sortedBy { it.daysUntil }
        val message = buildMessage(sorted)

        val single = sorted.singleOrNull()
        val result = fcmPushService.sendToUser(
            userId,
            message.title,
            message.body,
            mapOf("deepLink" to notificationDeepLinkService.resolve(single?.itemId)),
        )
        if (result.outcome == FcmSendOutcome.FAILED) {
            log.warn("전송에 실패해 알림 상태를 확정하지 않는다. userId={}, 실패 기기 수={}", userId, result.failedCount)
            return result.outcome
        }

        transaction.executeWithoutResult {
            val notification = notificationRepository.save(
                Notification(
                    userId = userId,
                    type = sorted.first().primaryType,
                    title = message.title,
                    body = message.body,
                    itemId = single?.itemId,
                    label = single?.label(),
                    nextReplacementDate = single?.nextReplacementDate,
                ),
            )
            notificationEntryRepository.saveAll(
                sorted.mapIndexed { index, candidate -> candidate.toEntry(notification, index) },
            )
            sorted.forEach { recordSent(it, today) }
            userNotificationSettingsService.recordDispatch(userId, today)
        }

        return result.outcome
    }

    /** 사유마다 상태를 확정한다. 지연은 스텝 진행, 여분 부족은 재입고 전 1회 기록. 사전·당일은 기록하지 않는다. */
    private fun recordSent(candidate: NotificationCandidate, today: LocalDate) {
        candidate.reasons.forEach { recordSent(candidate.itemId, it, today) }
    }

    private fun recordSent(itemId: Long, reason: NotificationType, today: LocalDate) {
        when (reason) {
            NotificationType.OVERDUE -> itemService.recordOverdueNotification(itemId, today)
            NotificationType.LOW_STOCK -> itemService.recordLowStockNotification(itemId, today)
            NotificationType.PRE_REPLACEMENT -> Unit
            NotificationType.NOTICE -> error("공지는 정책 판정 대상이 아니다.")
        }
    }

    private fun buildMessage(sorted: List<NotificationCandidate>): NotificationMessage =
        if (sorted.size == 1) singleMessage(sorted.single()) else bundleMessage(sorted)

    private fun NotificationCandidate.toEntry(notification: Notification, displayOrder: Int): NotificationEntry {
        val message = singleMessage(this)
        return NotificationEntry(
            notification = notification,
            type = primaryType,
            itemId = itemId,
            title = message.title,
            body = message.body,
            itemName = itemName,
            label = label(),
            nextReplacementDate = nextReplacementDate,
            displayOrder = displayOrder,
        )
    }

    /** 일정 사유가 있으면 그 문구에 여분 부족을 덧붙이고, 여분 부족만 있으면 여분 부족 문구를 쓴다. */
    private fun singleMessage(candidate: NotificationCandidate): NotificationMessage {
        val schedule = candidate.scheduleReason?.let { scheduleMessage(candidate, it) }
            ?: return NotificationMessage(
                title = "여분이 부족해요",
                body = "${candidate.itemName} 여분이 없어요. 지금 준비해두세요",
            )
        if (!candidate.hasLowStock) return schedule

        return schedule.copy(body = "${schedule.body} 여분도 없어요")
    }

    private fun scheduleMessage(candidate: NotificationCandidate, reason: NotificationType): NotificationMessage =
        when (reason) {
            NotificationType.PRE_REPLACEMENT -> if (candidate.daysUntil == 0) {
                NotificationMessage(
                    title = "오늘 교체할 날이에요",
                    body = "${candidate.itemName} 교체 예정일이 오늘이에요",
                )
            } else {
                NotificationMessage(
                    title = "교체 시기가 다가와요",
                    body = "${candidate.itemName} 교체가 얼마 남지 않았어요",
                )
            }
            NotificationType.OVERDUE -> NotificationMessage(
                title = "교체 시기가 지났어요",
                body = "${candidate.itemName} 교체 예정일이 지났어요",
            )
            NotificationType.LOW_STOCK, NotificationType.NOTICE -> error("일정 사유가 아니다: $reason")
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
