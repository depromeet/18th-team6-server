package depromeet.hotsix.obrit.global.log.access.service

import depromeet.hotsix.obrit.global.log.access.dto.AccessLogRecord
import depromeet.hotsix.obrit.global.log.access.entity.ApiAccessLog
import depromeet.hotsix.obrit.global.log.access.repository.ApiAccessLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class AccessLogService(private val repository: ApiAccessLogRepository) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun record(record: AccessLogRecord) {
        repository.save(
            ApiAccessLog(
                userId = record.userId,
                method = record.method,
                path = record.path,
                pathTemplate = record.pathTemplate,
                statusCode = record.statusCode,
                durationMs = record.durationMs,
                occurredAt = record.occurredAt,
            ),
        )
    }
}
