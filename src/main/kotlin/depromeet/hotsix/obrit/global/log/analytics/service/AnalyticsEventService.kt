package depromeet.hotsix.obrit.global.log.analytics.service

import depromeet.hotsix.obrit.global.log.analytics.entity.AnalyticsEventEntity
import depromeet.hotsix.obrit.global.log.analytics.repository.AnalyticsEventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.UUID

@Service
class AnalyticsEventService(private val repository: AnalyticsEventRepository, private val objectMapper: ObjectMapper) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun publishSignupCompleted(userId: Long, signupMethod: String) {
        publish(
            AnalyticsEvent(
                eventName = SIGNUP_COMPLETED_EVENT_NAME,
                userId = userId,
                properties = mapOf("signup_method" to signupMethod),
            ),
        )
    }

    private fun publish(event: AnalyticsEvent) {
        repository.save(
            AnalyticsEventEntity(
                eventId = event.eventId,
                eventName = event.eventName,
                userId = event.userId,
                occurredAt = event.occurredAt,
                properties = objectMapper.writeValueAsString(event.properties),
            ),
        )
    }

    companion object {
        private const val SIGNUP_COMPLETED_EVENT_NAME = "signup_completed"
    }
}

private data class AnalyticsEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val eventName: String,
    val userId: Long?,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
    val properties: Map<String, Any?>,
)
