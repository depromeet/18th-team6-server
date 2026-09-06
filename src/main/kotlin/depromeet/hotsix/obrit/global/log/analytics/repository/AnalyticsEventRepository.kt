package depromeet.hotsix.obrit.global.log.analytics.repository

import depromeet.hotsix.obrit.global.log.analytics.entity.AnalyticsEventEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface AnalyticsEventRepository : JpaRepository<AnalyticsEventEntity, Long> {

    @Query(
        """
        select e
        from AnalyticsEventEntity e
        where e.eventName = :eventName
          and e.occurredAt >= :startAt
          and e.occurredAt < :endAt
        order by e.occurredAt asc, e.id asc
        """,
    )
    fun findByEventNameAndOccurredAtWindow(
        @Param("eventName") eventName: String,
        @Param("startAt") startAt: LocalDateTime,
        @Param("endAt") endAt: LocalDateTime,
    ): List<AnalyticsEventEntity>
}
