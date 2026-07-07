package depromeet.hotsix.obrit.receipt.repository

import depromeet.hotsix.obrit.receipt.entity.ReceiptJob
import depromeet.hotsix.obrit.receipt.entity.ReceiptJobStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface ReceiptJobRepository : JpaRepository<ReceiptJob, Long> {

    fun findFirstByStatusOrderByIdAsc(status: ReceiptJobStatus): ReceiptJob?

    fun findAllByStatusAndUpdatedAtBefore(status: ReceiptJobStatus, threshold: LocalDateTime): List<ReceiptJob>
}
