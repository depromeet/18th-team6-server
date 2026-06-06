package depromeet.hotsix.obrit.accesslog.repository

import depromeet.hotsix.obrit.accesslog.entity.ApiAccessLog
import org.springframework.data.jpa.repository.JpaRepository

interface ApiAccessLogRepository : JpaRepository<ApiAccessLog, Long>
