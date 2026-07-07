package depromeet.hotsix.obrit.receipt.service

import depromeet.hotsix.obrit.receipt.client.StubReceiptOcrClient
import depromeet.hotsix.obrit.receipt.dto.OcrAnalysisResponse
import depromeet.hotsix.obrit.receipt.entity.ReceiptJobStatus
import depromeet.hotsix.obrit.receipt.repository.ReceiptJobRepository
import depromeet.hotsix.obrit.user.entity.UserFixture
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReceiptJobSchedulerServiceTest {

    @Autowired
    private lateinit var receiptJobSchedulerService: ReceiptJobSchedulerService

    @Autowired
    private lateinit var receiptJobService: ReceiptJobService

    @Autowired
    private lateinit var receiptJobRepository: ReceiptJobRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var stubReceiptOcrClient: StubReceiptOcrClient

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setUp() {
        stubReceiptOcrClient.reset()
    }

    @Test
    fun `poll은_토큰이_있으면_대기중_잡을_선점_처리해_완료시킨다`() {
        seedDefaultIcon()
        val user = userRepository.save(UserFixture.user())
        val jobId = receiptJobService.enqueue(
            userId = user.id!!,
            imageFile = MockMultipartFile("image", "receipt.jpg", "image/jpeg", "img".toByteArray()),
        )
        stubReceiptOcrClient.response = OcrAnalysisResponse(date = "2026-01-01")

        receiptJobSchedulerService.poll()

        assertEquals(ReceiptJobStatus.COMPLETED, receiptJobRepository.findById(jobId).get().status)
    }

    private fun seedDefaultIcon() {
        jdbcTemplate.update(
            "INSERT INTO icons (id, name, icon_key, url, created_at, updated_at) " +
                "VALUES (1, 'default', 'icons/default.png', 'https://cdn.example.com/icons/default.png', " +
                "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        )
    }
}
