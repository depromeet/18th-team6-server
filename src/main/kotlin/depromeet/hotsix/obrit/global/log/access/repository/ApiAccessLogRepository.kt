package depromeet.hotsix.obrit.global.log.access.repository

import depromeet.hotsix.obrit.global.log.access.entity.ApiAccessLog
import org.springframework.data.jpa.repository.JpaRepository

interface ApiAccessLogRepository : JpaRepository<ApiAccessLog, Long>
