package depromeet.hotsix.obrit.receipt.repository

import depromeet.hotsix.obrit.receipt.entity.ReceiptJob
import depromeet.hotsix.obrit.receipt.entity.ReceiptJobStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReceiptJobRepositoryTest {

    @Autowired
    private lateinit var receiptJobRepository: ReceiptJobRepository

    @Test
    fun `findByStatusOrderByIdAsc는_해당_상태만_id_오름차순으로_반환한다`() {
        receiptJobRepository.save(receiptJob(status = ReceiptJobStatus.PROCESSING))
        val firstPending = receiptJobRepository.save(receiptJob(status = ReceiptJobStatus.PENDING))
        val secondPending = receiptJobRepository.save(receiptJob(status = ReceiptJobStatus.PENDING))

        val picked = receiptJobRepository.findByStatusOrderByIdAsc(
            ReceiptJobStatus.PENDING,
            PageRequest.of(0, 10),
        )

        assertEquals(listOf(firstPending.id, secondPending.id), picked.map { it.id })
    }

    @Test
    fun `findByStatusOrderByIdAsc는_pageable_크기만큼만_반환한다`() {
        repeat(3) { receiptJobRepository.save(receiptJob(status = ReceiptJobStatus.PENDING)) }

        val picked = receiptJobRepository.findByStatusOrderByIdAsc(
            ReceiptJobStatus.PENDING,
            PageRequest.of(0, 2),
        )

        assertEquals(2, picked.size)
    }

    private fun receiptJob(status: ReceiptJobStatus) = ReceiptJob(
        userId = 1L,
        imageKey = "receipts/${status.name}.jpg",
        mimeType = "image/jpeg",
        status = status,
    )
}
