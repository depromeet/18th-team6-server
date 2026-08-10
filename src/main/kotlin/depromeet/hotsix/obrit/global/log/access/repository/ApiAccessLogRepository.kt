package depromeet.hotsix.obrit.global.log.access.repository

import depromeet.hotsix.obrit.global.log.access.entity.ApiAccessLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ApiAccessLogRepository : JpaRepository<ApiAccessLog, Long> {

    @Query(
        """
        select l
        from ApiAccessLog l
        where l.userId in :userIds
          and l.occurredAt >= :startAt
          and l.occurredAt < :endAt
        order by l.userId asc, l.occurredAt asc, l.id asc
        """,
    )
    fun findByUserIdsAndOccurredAtWindow(
        @Param("userIds") userIds: Collection<Long>,
        @Param("startAt") startAt: LocalDateTime,
        @Param("endAt") endAt: LocalDateTime,
    ): List<ApiAccessLog>
}
