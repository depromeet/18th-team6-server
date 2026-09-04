package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.repository.ItemRepository
import depromeet.hotsix.obrit.notification.repository.NotificationRepository
import depromeet.hotsix.obrit.user.entity.UserFixture
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationSchedulerServiceTest {

    @Autowired
    private lateinit var notificationSchedulerService: NotificationSchedulerService

    @Autowired
    private lateinit var notificationSettingsService: NotificationSettingsService

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var clock: Clock

    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        userId = requireNotNull(userRepository.save(UserFixture.user(id = null)).id)
        val today = LocalDate.now(clock)
        itemRepository.save(
            Item(
                userId = userId,
                categoryId = 1L,
                name = "수건",
                quantity = 2,
                replacementIntervalDays = 30,
                lastReplacedDate = today.minusDays(31),
                nextReplacementDate = today.minusDays(1),
            ),
        )
    }

    @Test
    fun `자동 발송이 꺼져 있으면 대상이 있어도 발송하지 않는다`() {
        notificationSettingsService.updateAutoDispatch(enabled = false)

        notificationSchedulerService.run()

        assertEquals(0, notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size)
    }

    @Test
    fun `자동 발송을 켜면 스케줄 실행 시 발송한다`() {
        notificationSettingsService.updateAutoDispatch(enabled = true)

        notificationSchedulerService.run()

        assertEquals(1, notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size)
    }

    @Test
    fun `기본 설정은 자동 발송이 꺼져 있다`() {
        assertEquals(false, notificationSettingsService.current().autoDispatchEnabled)
    }
}
