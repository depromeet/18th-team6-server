package depromeet.hotsix.obrit.global.log.analytics.repository

import depromeet.hotsix.obrit.global.log.analytics.entity.AnalyticsEventEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AnalyticsEventRepository : JpaRepository<AnalyticsEventEntity, Long>
