package depromeet.hotsix.obrit.global.log.analytics.service

import depromeet.hotsix.obrit.global.log.analytics.entity.AnalyticsEventEntity
import depromeet.hotsix.obrit.global.log.analytics.event.AnalyticsEvent
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
    fun publish(event: AnalyticsEvent) {
        repository.save(
            AnalyticsEventEntity(
                eventId = UUID.randomUUID().toString(),
                eventName = event.eventName.value,
                userId = event.userId,
                occurredAt = LocalDateTime.now(),
                properties = objectMapper.writeValueAsString(event.properties()),
            ),
        )
    }
}
