package depromeet.hotsix.obrit.admin.service

import depromeet.hotsix.obrit.admin.dto.AdminUserJourneyView
import depromeet.hotsix.obrit.global.log.access.entity.ApiAccessLog
import depromeet.hotsix.obrit.global.log.access.repository.ApiAccessLogRepository
import depromeet.hotsix.obrit.global.log.analytics.entity.AnalyticsEventEntity
import depromeet.hotsix.obrit.global.log.analytics.repository.AnalyticsEventRepository
import depromeet.hotsix.obrit.user.entity.UserFixture
import depromeet.hotsix.obrit.user.repository.UserRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminUserJourneyServiceTest {

    @Autowired
    private lateinit var service: AdminUserJourneyService

    @Autowired
    private lateinit var apiAccessLogRepository: ApiAccessLogRepository

    @Autowired
    private lateinit var analyticsEventRepository: AnalyticsEventRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val windowStart: LocalDateTime = LocalDateTime.of(2026, 3, 1, 0, 0)
    private val windowEnd: LocalDateTime = LocalDateTime.of(2026, 4, 1, 0, 0)

    @BeforeEach
    fun setUp() {
        apiAccessLogRepository.deleteAll()
        analyticsEventRepository.deleteAll()
    }

    @Test
    fun `핵심_액션은_사용자_단위로_집계된다`() {
        val userA = saveUser()
        val userB = saveUser()

        // A는 등록·교체, B는 등록만. 등록 2명, 교체 1명이 되어야 한다.
        saveLog(userA, "POST", "/items")
        saveLog(userA, "POST", "/items")
        saveLog(userA, "POST", "/items/{itemId}/replacements")
        saveLog(userB, "POST", "/items/bulk")

        val view = service.getUserJourney(windowStart, windowEnd)

        assertEquals(2, view.activeUsers)
        assertEquals(2, actionRow(view, "register").users)
        assertEquals("100.0%", actionRow(view, "register").userRate)
        assertEquals(1, actionRow(view, "replace").users)
        assertEquals("50.0%", actionRow(view, "replace").userRate)
        assertEquals(0, actionRow(view, "delete").users)
    }

    @Test
    fun `실패한_요청은_액션으로_치지_않는다`() {
        val userId = saveUser()
        saveLog(userId, "POST", "/items", statusCode = 400)

        val view = service.getUserJourney(windowStart, windowEnd)

        assertEquals(0, view.activeUsers)
        assertEquals(0, actionRow(view, "register").users)
    }

    @Test
    fun `영수증_이벤트에서_성공률과_실패_사유를_집계한다`() {
        val userId = saveUser()
        saveLog(userId, "POST", "/receipts/analyze")
        saveReceiptEvent(userId, success = true, totalMs = 1000, ocrMs = 800)
        saveReceiptEvent(userId, success = true, totalMs = 2000, ocrMs = 1600)
        saveReceiptEvent(userId, success = false, totalMs = 300, ocrMs = null, failureReason = "INVALID_FILE")
        saveReceiptEvent(userId, success = false, totalMs = 500, ocrMs = 400, failureReason = "UPSTREAM_5XX")
        saveReceiptEvent(userId, success = false, totalMs = 700, ocrMs = 600, failureReason = "UPSTREAM_5XX")

        val view = service.getUserJourney(windowStart, windowEnd)

        assertEquals(5, view.receipt.total)
        assertEquals(2, view.receipt.success)
        assertEquals(3, view.receipt.failure)
        assertEquals("40.0%", view.receipt.successRate)
        assertEquals("1500ms", view.receipt.avgSuccessMs)
        assertEquals("500ms", view.receipt.avgFailureMs)

        // 건수가 많은 사유가 먼저 온다
        assertEquals("UPSTREAM_5XX", view.receiptFailureRows[0].reason)
        assertEquals(2, view.receiptFailureRows[0].count)
        assertEquals("INVALID_FILE", view.receiptFailureRows[1].reason)
    }

    @Test
    fun `영수증_이벤트가_없으면_평균은_대시로_표시된다`() {
        saveUser()

        val view = service.getUserJourney(windowStart, windowEnd)

        assertEquals(0, view.receipt.total)
        assertEquals("-", view.receipt.successRate)
        assertEquals("-", view.receipt.avgSuccessMs)
        assertEquals(0, view.receiptFailureRows.size)
    }

    @Test
    fun `가입일과_다른_날_접속하면_재방문으로_집계된다`() {
        val signedUpAt = windowStart.plusDays(1)
        val revisitedUser = saveUser(createdAt = signedUpAt)
        val sameDayUser = saveUser(createdAt = signedUpAt)

        saveLog(revisitedUser, "GET", "/home/items", occurredAt = signedUpAt)
        saveLog(revisitedUser, "GET", "/home/items", occurredAt = signedUpAt.plusDays(3))
        saveLog(sameDayUser, "GET", "/home/items", occurredAt = signedUpAt.plusHours(2))

        val view = service.getUserJourney(windowStart, windowEnd)

        assertEquals(2, view.revisit.cohortUsers)
        assertEquals(1, view.revisit.revisitedUsers)
        assertEquals("50.0%", view.revisit.revisitRate)
    }

    @Test
    fun `필드가_빠진_구형_이벤트도_집계에서_빠지지_않는다`() {
        val userId = saveUser()
        // 스키마가 바뀌기 전에 쌓인 이벤트처럼 total_ms·failure_reason이 없는 경우
        saveRawReceiptEvent(userId, "{\"success\":false}")
        saveReceiptEvent(userId, success = false, totalMs = 500, ocrMs = 400, failureReason = "TIMEOUT")

        val view = service.getUserJourney(windowStart, windowEnd)

        assertEquals(2, view.receipt.total)
        assertEquals(2, view.receipt.failure)
        // total_ms가 없는 건은 평균에서만 빠지고, 실패 사유는 UNKNOWN으로 묶인다
        assertEquals("500ms", view.receipt.avgFailureMs)
        assertEquals(setOf("TIMEOUT", "UNKNOWN"), view.receiptFailureRows.map { it.reason }.toSet())
    }

    private fun actionRow(view: AdminUserJourneyView, action: String) =
        view.coreActionRows.first { it.action == action }

    /**
     * created_at은 [BaseTimeEntity]에서 `updatable = false`라 JPA로 덮어쓸 수 없어
     * 코호트 판정용 시각을 JDBC로 직접 밀어 넣는다.
     */
    private fun saveUser(createdAt: LocalDateTime = windowStart.plusDays(1)): Long {
        val user = userRepository.saveAndFlush(UserFixture.user(id = null))
        val userId = requireNotNull(user.id)
        jdbcTemplate.update("UPDATE users SET created_at = ? WHERE id = ?", createdAt, userId)
        entityManager.clear()
        return userId
    }

    private fun saveLog(
        userId: Long,
        method: String,
        pathTemplate: String,
        statusCode: Int = 200,
        occurredAt: LocalDateTime = windowStart.plusDays(2),
    ) {
        apiAccessLogRepository.save(
            ApiAccessLog(
                userId = userId,
                method = method,
                path = pathTemplate,
                pathTemplate = pathTemplate,
                statusCode = statusCode,
                durationMs = 10,
                occurredAt = occurredAt,
            ),
        )
    }

    private fun saveReceiptEvent(
        userId: Long,
        success: Boolean,
        totalMs: Long,
        ocrMs: Long?,
        failureReason: String? = null,
    ) {
        val properties = buildString {
            append("{\"success\":$success,\"total_ms\":$totalMs,")
            append("\"ocr_ms\":${ocrMs ?: "null"},\"detected_item_count\":null,")
            append("\"failure_reason\":${failureReason?.let { "\"$it\"" } ?: "null"}}")
        }
        saveRawReceiptEvent(userId, properties)
    }

    private fun saveRawReceiptEvent(userId: Long, properties: String) {
        analyticsEventRepository.save(
            AnalyticsEventEntity(
                eventId = UUID.randomUUID().toString(),
                eventName = "receipt_analysis_finished",
                userId = userId,
                occurredAt = windowStart.plusDays(2),
                properties = properties,
            ),
        )
    }
}
