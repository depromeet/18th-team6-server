package depromeet.hotsix.obrit.receipt.repository

import depromeet.hotsix.obrit.receipt.entity.ReceiptJob
import depromeet.hotsix.obrit.receipt.entity.ReceiptJobStatus
import org.springframework.data.jpa.repository.JpaRepository

interface ReceiptJobRepository : JpaRepository<ReceiptJob, Long> {

    fun findFirstByStatusOrderByIdAsc(status: ReceiptJobStatus): ReceiptJob?
}
