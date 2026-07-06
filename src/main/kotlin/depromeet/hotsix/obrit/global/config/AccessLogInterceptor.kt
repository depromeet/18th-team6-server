package depromeet.hotsix.obrit.global.config

import depromeet.hotsix.obrit.global.log.access.dto.AccessLogRecord
import depromeet.hotsix.obrit.global.log.access.service.AccessLogService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping
import java.time.LocalDateTime

@Component
class AccessLogInterceptor(private val accessLogService: AccessLogService) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        request.setAttribute(ATTR_START_NANOS, System.nanoTime())
        request.setAttribute(ATTR_OCCURRED_AT, LocalDateTime.now())
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        try {
            val startNanos = request.getAttribute(ATTR_START_NANOS) as? Long ?: return
            val occurredAt = request.getAttribute(ATTR_OCCURRED_AT) as? LocalDateTime ?: return

            val durationMs = ((System.nanoTime() - startNanos) / 1_000_000).toInt()
            val pathTemplate = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as? String
                ?: request.requestURI

            accessLogService.record(
                AccessLogRecord(
                    userId = resolveUserId(request),
                    method = request.method,
                    path = request.requestURI,
                    pathTemplate = pathTemplate,
                    statusCode = response.status,
                    durationMs = durationMs,
                    occurredAt = occurredAt,
                ),
            )
        } catch (t: Throwable) {
            log.warn("Failed to record api access log", t)
        }
    }

    private fun resolveUserId(request: HttpServletRequest): Long? = request.getHeader("X-User-Id")?.toLongOrNull()

    companion object {
        private const val ATTR_START_NANOS = "accesslog.startNanos"
        private const val ATTR_OCCURRED_AT = "accesslog.occurredAt"
        private val log = LoggerFactory.getLogger(AccessLogInterceptor::class.java)
    }
}
