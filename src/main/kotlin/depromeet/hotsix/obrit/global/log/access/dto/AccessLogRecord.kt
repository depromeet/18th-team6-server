package depromeet.hotsix.obrit.global.log.access.dto

import java.time.LocalDateTime

data class AccessLogRecord(
    val userId: Long?,
    val method: String,
    val path: String,
    val pathTemplate: String,
    val statusCode: Int,
    val durationMs: Int,
    val occurredAt: LocalDateTime,
)
