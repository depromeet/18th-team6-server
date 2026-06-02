package depromeet.hotsix.obrit.global.log.analytics.service

import depromeet.hotsix.obrit.global.log.analytics.entity.AnalyticsEventEntity
import depromeet.hotsix.obrit.global.log.analytics.repository.AnalyticsEventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun publishItemRegistered(
        userId: Long,
        itemId: Long,
        categoryId: Long,
        replacementIntervalDays: Int,
        source: String,
    ) {
        publish(
            AnalyticsEvent(
                eventName = ITEM_REGISTERED_EVENT_NAME,
                userId = userId,
                properties = mapOf(
                    "item_id" to itemId,
                    "category_id" to categoryId,
                    "replacement_interval_days" to replacementIntervalDays,
                    "source" to source,
                ),
            ),
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun publishReplacementRecorded(
        userId: Long,
        itemId: Long,
        replacementHistoryId: Long,
        replacedDate: LocalDate,
        daysSinceLastReplacement: Int,
    ) {
        publish(
            AnalyticsEvent(
                eventName = REPLACEMENT_RECORDED_EVENT_NAME,
                userId = userId,
                properties = mapOf(
                    "item_id" to itemId,
                    "replacement_history_id" to replacementHistoryId,
                    "replaced_date" to replacedDate.toString(),
                    "days_since_last_replacement" to daysSinceLastReplacement,
                ),
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
        private const val ITEM_REGISTERED_EVENT_NAME = "item_registered"
        private const val REPLACEMENT_RECORDED_EVENT_NAME = "replacement_recorded"
    }

    private data class AnalyticsEvent(
        val eventId: String = UUID.randomUUID().toString(),
        val eventName: String,
        val userId: Long?,
        val occurredAt: LocalDateTime = LocalDateTime.now(),
        val properties: Map<String, Any?>,
    )
}
