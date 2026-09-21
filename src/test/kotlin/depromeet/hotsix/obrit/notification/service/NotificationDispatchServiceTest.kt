package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.repository.ItemRepository
import depromeet.hotsix.obrit.notification.entity.Notification
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
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

    @Autowired
    private lateinit var notificationService: NotificationService

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
        assertEquals(item.id, notifications.single().itemId)
        assertEquals("교체 D+1", notifications.single().label)
        assertEquals(today.minusDays(1), notifications.single().nextReplacementDate)
        val response = notificationService.listAllNotification(userId).single()
        assertEquals("교체 D+1", response.label)
        assertEquals(today.minusDays(1), response.nextReplacementDate)
        assertEquals(item.id, response.itemId)
        assertEquals("obrit://items/${item.id}", response.deepLink)
        val read = notificationService.markAsRead(userId, response.id)
        assertEquals(response.deepLink, read.deepLink)
        assertEquals(response.itemId, read.itemId)
        assertTrue(read.isRead)
        assertEquals(read.readAt, notificationService.markAsRead(userId, response.id).readAt)

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
        val response = notificationService.listAllNotification(userId).single()
        assertNull(response.itemId)
        assertNull(response.label)
        assertNull(response.nextReplacementDate)
        assertEquals("obrit://home", response.deepLink)
        assertEquals("obrit://home", notificationService.markAsRead(userId, response.id).deepLink)

        assertEquals(today, itemRepository.getReferenceById(requireNotNull(urgent.id)).lowStockNotifiedAt)
        assertEquals(today, itemRepository.getReferenceById(requireNotNull(other.id)).lowStockNotifiedAt)
    }

    @Test
    fun `발송 대상이 없으면 알림을 생성하지 않는다`() {
        saveItem(name = "칫솔", quantity = 2, nextReplacementDate = today.plusDays(10))

        notificationDispatchService.dispatch()

        assertEquals(0, notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size)
    }

    @Test
    fun `여분 부족 알림은 소모품 변경 후에도 발송 당시 정보를 반환한다`() {
        val item = saveItem("치실", 0, today.plusDays(1))
        notificationDispatchService.dispatch()
        item.nextReplacementDate = today.plusDays(30)
        item.quantity = 5
        itemRepository.flush()

        val response = notificationService.listAllNotification(userId).single()
        assertEquals("여분 부족", response.label)
        assertEquals(today.plusDays(1), response.nextReplacementDate)
        assertEquals(item.id, response.itemId)
    }

    @Test
    fun `사전 알림은 교체 D 마이너스 라벨을 반환한다`() {
        saveItem("칫솔", 2, today.plusDays(3))
        notificationDispatchService.dispatch()
        assertEquals("교체 D-3", notificationService.listAllNotification(userId).single().label)
    }

    @Test
    fun `기존 알림과 공지는 홈으로 이동하고 다른 사용자는 읽을 수 없다`() {
        val notification = notificationRepository.save(Notification(userId = userId, type = NotificationType.NOTICE))
        val response = notificationService.listAllNotification(userId).single()
        assertNull(response.label)
        assertNull(response.nextReplacementDate)
        assertNull(response.itemId)
        assertEquals("obrit://home", response.deepLink)
        assertEquals("obrit://home", notificationService.markAsRead(userId, response.id).deepLink)
        assertFailsWith<ResourceNotFoundException> {
            notificationService.markAsRead(userId + 10000, requireNotNull(notification.id))
        }
    }
}
