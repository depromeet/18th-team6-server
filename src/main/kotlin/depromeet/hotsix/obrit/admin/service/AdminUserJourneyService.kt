package depromeet.hotsix.obrit.admin.service

import depromeet.hotsix.obrit.admin.dto.AdminCoreActionRow
import depromeet.hotsix.obrit.admin.dto.AdminReceiptAnalysisSummary
import depromeet.hotsix.obrit.admin.dto.AdminReceiptFailureRow
import depromeet.hotsix.obrit.admin.dto.AdminRevisitSummary
import depromeet.hotsix.obrit.admin.dto.AdminUserJourneyUserRow
import depromeet.hotsix.obrit.admin.dto.AdminUserJourneyView
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.global.log.access.entity.ApiAccessLog
import depromeet.hotsix.obrit.global.log.access.repository.ApiAccessLogRepository
import depromeet.hotsix.obrit.global.log.analytics.entity.AnalyticsEventEntity
import depromeet.hotsix.obrit.global.log.analytics.event.AnalyticsEventName
import depromeet.hotsix.obrit.global.log.analytics.repository.AnalyticsEventRepository
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.LocalDateTime

private const val DEFAULT_WINDOW_DAYS = 30L
private const val REVISIT_WINDOW_DAYS = 30L

/**
 * 사용자 저니 지표를 모은다.
 *
 * 대부분의 지표는 api_access_logs의 pathTemplate으로 답이 나오므로 별도 수집을 두지 않는다.
 * 접근 로그로 알 수 없는 영수증 분석 소요 시간·실패 사유만 analytics_events에서 읽는다.
 */
@Service
class AdminUserJourneyService(
    private val apiAccessLogRepository: ApiAccessLogRepository,
    private val analyticsEventRepository: AnalyticsEventRepository,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getUserJourney(startAt: LocalDateTime?, endAt: LocalDateTime?): AdminUserJourneyView {
        val windowEnd = endAt ?: LocalDateTime.now(clock)
        val windowStart = startAt ?: windowEnd.minusDays(DEFAULT_WINDOW_DAYS)
        if (!windowEnd.isAfter(windowStart)) {
            throw BusinessException("종료 시각은 시작 시각보다 뒤여야 합니다.")
        }

        val logs = apiAccessLogRepository.findLoggedInByOccurredAtWindow(windowStart, windowEnd)
        val successLogsByUser = logs.filter { it.statusCode < 400 }.groupBy { requireNotNull(it.userId) }
        val activeUsers = successLogsByUser.keys

        val receiptEvents = analyticsEventRepository.findByEventNameAndOccurredAtWindow(
            eventName = AnalyticsEventName.RECEIPT_ANALYSIS_FINISHED.value,
            startAt = windowStart,
            endAt = windowEnd,
        ).mapNotNull { it.toReceiptAnalysis() }

        val signedUpAtByUser = userRepository.findActiveCreatedBetween(windowStart, windowEnd)
            .associate { requireNotNull(it.id) to requireNotNull(it.createdAt) }

        val userRows = activeUsers.sorted().map { userId ->
            buildUserRow(userId, successLogsByUser.getValue(userId), signedUpAtByUser[userId])
        }

        return AdminUserJourneyView(
            startAt = windowStart,
            endAt = windowEnd,
            activeUsers = activeUsers.size,
            coreActionRows = buildCoreActionRows(userRows),
            receipt = summarizeReceipt(receiptEvents),
            receiptFailureRows = buildFailureRows(receiptEvents),
            revisit = summarizeRevisit(userRows),
            userRows = userRows,
        )
    }

    private fun buildUserRow(
        userId: Long,
        successLogs: List<ApiAccessLog>,
        signedUpAt: LocalDateTime?,
    ): AdminUserJourneyUserRow {
        val lastSeenAt = successLogs.maxOfOrNull { it.occurredAt }
        // 가입 코호트에 한해, 가입 후 30일 안에 가입일과 다른 날 접속했으면 재방문으로 본다.
        val revisited = signedUpAt != null &&
            successLogs.any {
                it.occurredAt.toLocalDate() != signedUpAt.toLocalDate() &&
                    it.occurredAt.isBefore(signedUpAt.plusDays(REVISIT_WINDOW_DAYS))
            }

        return AdminUserJourneyUserRow(
            userId = userId,
            signedUpAt = signedUpAt,
            registered = successLogs.any { it.isRegisterItem() },
            edited = successLogs.any { it.isEditItem() },
            replaced = successLogs.any { it.isReplaceItem() },
            spareUpdated = successLogs.any { it.isUpdateSpareCount() },
            deleted = successLogs.any { it.isDeleteItem() },
            usedReceipt = successLogs.any { it.isAnalyzeReceipt() },
            revisited = revisited,
            lastSeenAt = lastSeenAt,
        )
    }

    private fun buildCoreActionRows(users: List<AdminUserJourneyUserRow>): List<AdminCoreActionRow> {
        val total = users.size
        return listOf(
            Triple("register", "소모품 등록", users.count { it.registered }),
            Triple("edit", "소모품 편집", users.count { it.edited }),
            Triple("replace", "교체 완료", users.count { it.replaced }),
            Triple("spare_update", "여분 수정", users.count { it.spareUpdated }),
            Triple("delete", "소모품 삭제", users.count { it.deleted }),
            Triple("receipt", "영수증 등록", users.count { it.usedReceipt }),
        ).map { (action, label, count) ->
            AdminCoreActionRow(action = action, label = label, users = count, userRate = percentage(count, total))
        }
    }

    private fun summarizeReceipt(events: List<ReceiptAnalysis>): AdminReceiptAnalysisSummary {
        val success = events.filter { it.success }
        val failure = events.filter { !it.success }
        return AdminReceiptAnalysisSummary(
            total = events.size,
            success = success.size,
            failure = failure.size,
            successRate = percentage(success.size, events.size),
            avgSuccessMs = averageMs(success.mapNotNull { it.totalMs }),
            avgFailureMs = averageMs(failure.mapNotNull { it.totalMs }),
            avgOcrMs = averageMs(events.mapNotNull { it.ocrMs }),
        )
    }

    private fun buildFailureRows(events: List<ReceiptAnalysis>): List<AdminReceiptFailureRow> {
        val failures = events.filter { !it.success }
        return failures.groupingBy { it.failureReason ?: "UNKNOWN" }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { (reason, count) ->
                AdminReceiptFailureRow(reason = reason, count = count, rate = percentage(count, failures.size))
            }
    }

    private fun summarizeRevisit(users: List<AdminUserJourneyUserRow>): AdminRevisitSummary {
        val cohort = users.filter { it.signedUpAt != null }
        val revisited = cohort.count { it.revisited }
        return AdminRevisitSummary(
            cohortUsers = cohort.size,
            revisitedUsers = revisited,
            revisitRate = percentage(revisited, cohort.size),
        )
    }

    /** properties가 깨져 있어도 화면 전체가 죽지 않도록 해당 건만 버린다. */
    private fun AnalyticsEventEntity.toReceiptAnalysis(): ReceiptAnalysis? = try {
        val node = objectMapper.readTree(properties)
        ReceiptAnalysis(
            success = node.path("success").asBoolean(false),
            totalMs = node.longOrNull("total_ms"),
            ocrMs = node.longOrNull("ocr_ms"),
            failureReason = node.path("failure_reason").takeIf { !it.isNull && !it.isMissingNode }?.asString(),
        )
    } catch (t: Throwable) {
        log.warn("analytics_events.properties 파싱에 실패했습니다. eventId=$eventId", t)
        null
    }

    private fun JsonNode.longOrNull(field: String): Long? = path(field).takeIf { it.isNumber }?.asLong()

    private data class ReceiptAnalysis(
        val success: Boolean,
        val totalMs: Long?,
        val ocrMs: Long?,
        val failureReason: String?,
    )
}

private fun averageMs(values: List<Long>): String = if (values.isEmpty()) "-" else "%.0fms".format(values.average())

private fun percentage(numerator: Int, denominator: Int): String = if (denominator == 0) {
    "-"
} else {
    "%.1f%%".format(numerator.toDouble() / denominator.toDouble() * 100.0)
}

private fun ApiAccessLog.isRegisterItem(): Boolean =
    method == "POST" && (pathTemplate == "/items" || pathTemplate == "/items/bulk")

private fun ApiAccessLog.isEditItem(): Boolean = method == "PATCH" && pathTemplate == "/items/{itemId}"

private fun ApiAccessLog.isReplaceItem(): Boolean = method == "POST" && pathTemplate == "/items/{itemId}/replacements"

private fun ApiAccessLog.isUpdateSpareCount(): Boolean =
    method == "PATCH" && pathTemplate == "/items/{itemId}/spare-count"

private fun ApiAccessLog.isDeleteItem(): Boolean = method == "DELETE" && pathTemplate == "/items/{itemId}"

private fun ApiAccessLog.isAnalyzeReceipt(): Boolean = method == "POST" && pathTemplate == "/receipts/analyze"
