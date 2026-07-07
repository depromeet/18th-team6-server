package depromeet.hotsix.obrit.receipt.repository

import depromeet.hotsix.obrit.receipt.entity.ReceiptJob
import depromeet.hotsix.obrit.receipt.entity.ReceiptJobStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReceiptJobRepositoryTest {

    @Autowired
    private lateinit var receiptJobRepository: ReceiptJobRepository

    @Test
    fun `findFirstByStatusOrderByIdAsc는_해당_상태_중_가장_작은_id를_반환한다`() {
        receiptJobRepository.save(receiptJob(status = ReceiptJobStatus.PROCESSING))
        val oldestPending = receiptJobRepository.save(receiptJob(status = ReceiptJobStatus.PENDING))
        receiptJobRepository.save(receiptJob(status = ReceiptJobStatus.PENDING))

        val picked = receiptJobRepository.findFirstByStatusOrderByIdAsc(ReceiptJobStatus.PENDING)

        assertEquals(oldestPending.id, picked?.id)
    }

    @Test
    fun `findFirstByStatusOrderByIdAsc는_해당_상태가_없으면_null을_반환한다`() {
        receiptJobRepository.save(receiptJob(status = ReceiptJobStatus.PROCESSING))

        val picked = receiptJobRepository.findFirstByStatusOrderByIdAsc(ReceiptJobStatus.PENDING)

        assertNull(picked)
    }

    private fun receiptJob(status: ReceiptJobStatus) = ReceiptJob(
        userId = 1L,
        imageKey = "receipts/${status.name}.jpg",
        mimeType = "image/jpeg",
        status = status,
    )
}
