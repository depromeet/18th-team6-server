package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.repository.ItemRepository
import depromeet.hotsix.obrit.notification.entity.NotificationType
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
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationDispatchServiceTest {

    @Autowired
    private lateinit var notificationDispatchService: NotificationDispatchService

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var clock: Clock

    private val today: LocalDate
        get() = LocalDate.now(clock)
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        userId = requireNotNull(userRepository.save(UserFixture.user(id = null)).id)
    }

    private fun saveItem(name: String, quantity: Int, nextReplacementDate: LocalDate): Item = itemRepository.save(
        Item(
            userId = userId,
            categoryId = 1L,
            name = name,
            quantity = quantity,
            replacementIntervalDays = 30,
            lastReplacedDate = nextReplacementDate.minusDays(30),
            nextReplacementDate = nextReplacementDate,
        ),
    )

    @Test
    fun `대상이 하나면 단건 알림을 발송하고 지연 발송 횟수를 기록한다`() {
        val item = saveItem(name = "수건", quantity = 2, nextReplacementDate = today.minusDays(1))

        notificationDispatchService.dispatch()

        val notifications = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
        assertEquals(1, notifications.size)
        assertEquals(NotificationType.OVERDUE, notifications.single().type)
        assertTrue(notifications.single().body.contains("수건"))

        val updated = itemRepository.getReferenceById(requireNotNull(item.id))
        assertEquals(1, updated.overdueNotifiedCount)
        assertEquals(today, updated.lastOverdueNotifiedAt)
    }

    @Test
    fun `같은 유저에게 대상이 둘 이상이면 묶음 알림 하나만 발송한다`() {
        val urgent = saveItem(name = "치실", quantity = 0, nextReplacementDate = today.plusDays(1))
        val other = saveItem(name = "면봉", quantity = 0, nextReplacementDate = today.plusDays(2))

        notificationDispatchService.dispatch()

        val notifications = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
        assertEquals(1, notifications.size)
        assertTrue(notifications.single().body.contains("치실"))
        assertTrue(notifications.single().body.contains("외 1건"))

        assertEquals(today, itemRepository.getReferenceById(requireNotNull(urgent.id)).lowStockNotifiedAt)
        assertEquals(today, itemRepository.getReferenceById(requireNotNull(other.id)).lowStockNotifiedAt)
    }

    @Test
    fun `발송 대상이 없으면 알림을 생성하지 않는다`() {
        saveItem(name = "칫솔", quantity = 2, nextReplacementDate = today.plusDays(10))

        notificationDispatchService.dispatch()

        assertEquals(0, notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size)
    }
}
