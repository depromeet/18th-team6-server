package depromeet.hotsix.obrit.global.log.analytics.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "analytics_events",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_analytics_events_event_id", columnNames = ["event_id"]),
    ],
    indexes = [
        Index(name = "idx_analytics_events_name_occurred", columnList = "event_name, occurred_at"),
        Index(name = "idx_analytics_events_user_occurred", columnList = "user_id, occurred_at"),
    ],
)
class AnalyticsEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "event_id", nullable = false, length = 36)
    var eventId: String,

    @Column(name = "event_name", nullable = false, length = 100)
    var eventName: String,

    @Column(name = "user_id")
    var userId: Long? = null,

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: LocalDateTime,

    @Column(name = "properties", nullable = false, columnDefinition = "JSON")
    var properties: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    constructor() : this(
        eventId = "",
        eventName = "",
        userId = null,
        occurredAt = LocalDateTime.MIN,
        properties = "{}",
        createdAt = LocalDateTime.now(),
    )
}
