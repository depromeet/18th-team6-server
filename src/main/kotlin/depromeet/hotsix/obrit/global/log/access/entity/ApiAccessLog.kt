package depromeet.hotsix.obrit.global.log.access.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "api_access_logs",
    indexes = [
        Index(name = "idx_api_access_logs_user_occurred", columnList = "user_id, occurred_at"),
        Index(name = "idx_api_access_logs_path_template_occurred", columnList = "path_template, occurred_at"),
    ],
)
class ApiAccessLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id")
    var userId: Long? = null,

    @Column(name = "method", nullable = false, length = 10)
    var method: String,

    @Column(name = "path", nullable = false, length = 255)
    var path: String,

    @Column(name = "path_template", nullable = false, length = 255)
    var pathTemplate: String,

    @Column(name = "status_code", nullable = false)
    var statusCode: Int,

    @Column(name = "duration_ms", nullable = false)
    var durationMs: Int,

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: LocalDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    constructor() : this(
        userId = null,
        method = "",
        path = "",
        pathTemplate = "",
        statusCode = 0,
        durationMs = 0,
        occurredAt = LocalDateTime.MIN,
        createdAt = LocalDateTime.now(),
    )
}
