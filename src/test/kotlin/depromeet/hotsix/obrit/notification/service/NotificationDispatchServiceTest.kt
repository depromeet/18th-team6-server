package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.repository.ItemRepository
import depromeet.hotsix.obrit.notification.UserNotificationSettingsFixture
import depromeet.hotsix.obrit.notification.entity.Notification
import depromeet.hotsix.obrit.notification.entity.NotificationType
import depromeet.hotsix.obrit.notification.repository.NotificationRepository
import depromeet.hotsix.obrit.notification.repository.UserNotificationSettingsRepository
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
import java.time.ZoneId
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
    private lateinit var userNotificationSettingsRepository: UserNotificationSettingsRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var clock: Clock

    @Autowired
    private lateinit var notificationService: NotificationService

    // 발송 서비스는 Asia/Seoul 기준 날짜를 쓴다. 시스템 시간대(CI는 UTC)로 구하면 KST 0~9시에 날짜가 하루 어긋난다.
    private val today: LocalDate
        get() = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")))
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        userId = requireNotNull(userRepository.save(UserFixture.user(id = null)).id)
        userNotificationSettingsRepository.save(UserNotificationSettingsFixture.allEnabled(userId))
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
        assertEquals(false, response.isBundled)
        assertEquals(1, response.cards.size)
        assertEquals(NotificationType.OVERDUE, response.cards.single().type)
        assertEquals("수건", response.cards.single().itemName)
        assertEquals("교체 D+1", response.cards.single().label)
        assertEquals("obrit://items/${item.id}", response.cards.single().deepLink)
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
        assertTrue(response.isBundled)
        assertEquals(listOf("치실", "면봉"), response.cards.map { it.itemName })
        // D-1은 사전 스텝이라 교체 예정이 대표 유형이고, D-2는 스텝이 아니라 여분 부족만 남는다.
        assertEquals(
            listOf(NotificationType.PRE_REPLACEMENT, NotificationType.LOW_STOCK),
            response.cards.map {
                it.type
            },
        )
        assertTrue(response.cards.all { it.hasLowStock })
        assertEquals(
            listOf("obrit://items/${urgent.id}", "obrit://items/${other.id}"),
            response.cards.map { it.deepLink },
        )
        assertEquals("obrit://home", notificationService.markAsRead(userId, response.id).deepLink)

        assertEquals(today, itemRepository.getReferenceById(requireNotNull(urgent.id)).lowStockNotifiedAt)
        assertEquals(today, itemRepository.getReferenceById(requireNotNull(other.id)).lowStockNotifiedAt)
    }

    // 서로 다른 유형의 후보도 사용자 단위 묶음 안에서 각 카드의 유형을 유지하는지 확인한다.
    @Test
    fun `서로 다른 유형의 후보도 카드별 상세 정보를 유지한다`() {
        val overdue = saveItem(name = "수건", quantity = 2, nextReplacementDate = today.minusDays(1))
        val upcoming = saveItem(name = "칫솔", quantity = 2, nextReplacementDate = today.plusDays(3))

        notificationDispatchService.dispatch()

        val response = notificationService.listAllNotification(userId).single()
        assertTrue(response.isBundled)
        assertEquals(listOf(NotificationType.OVERDUE, NotificationType.PRE_REPLACEMENT), response.cards.map { it.type })
        assertEquals(listOf(overdue.id, upcoming.id), response.cards.map { it.itemId })
        assertEquals(listOf("교체 D+1", "교체 D-3"), response.cards.map { it.label })
        assertTrue(response.cards.none { it.hasLowStock })
    }

    @Test
    fun `교체 예정과 여분 부족이 겹치면 한 알림으로 보내고 여분 부족 발송을 기록한다`() {
        val item = saveItem(name = "면도날", quantity = 0, nextReplacementDate = today.plusDays(3))

        notificationDispatchService.dispatch()

        val notification = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).single()
        assertEquals(NotificationType.PRE_REPLACEMENT, notification.type)
        assertEquals("교체 D-3 · 여분 부족", notification.label)
        assertEquals("교체 시기가 다가와요", notification.title)
        assertEquals("면도날 교체가 얼마 남지 않았어요 여분도 없어요", notification.body)
        val card = notificationService.listAllNotification(userId).single().cards.single()
        assertEquals(NotificationType.PRE_REPLACEMENT, card.type)
        assertTrue(card.hasLowStock)
        assertEquals("면도날 교체가 얼마 남지 않았어요 여분도 없어요", card.content)
        assertEquals(today, itemRepository.getReferenceById(requireNotNull(item.id)).lowStockNotifiedAt)
    }

    @Test
    fun `지연과 여분 부족이 겹치면 한 알림으로 보내고 지연 스텝과 여분 부족을 함께 기록한다`() {
        val item = saveItem(name = "수건", quantity = 0, nextReplacementDate = today.minusDays(1))

        notificationDispatchService.dispatch()

        val notification = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).single()
        assertEquals(NotificationType.OVERDUE, notification.type)
        assertEquals("교체 D+1 · 여분 부족", notification.label)
        val updated = itemRepository.getReferenceById(requireNotNull(item.id))
        assertEquals(1, updated.overdueNotifiedCount)
        assertEquals(today, updated.lastOverdueNotifiedAt)
        assertEquals(today, updated.lowStockNotifiedAt)
    }

    @Test
    fun `교체 예정일 당일에는 당일 문구로 보낸다`() {
        saveItem(name = "수세미", quantity = 2, nextReplacementDate = today)

        notificationDispatchService.dispatch()

        val notification = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).single()
        assertEquals(NotificationType.PRE_REPLACEMENT, notification.type)
        assertEquals("오늘 교체할 날이에요", notification.title)
        assertEquals("수세미 교체 예정일이 오늘이에요", notification.body)
        assertEquals("교체 D-day", notification.label)
    }

    @Test
    fun `여분 부족만 있으면 여분 부족 문구로 보낸다`() {
        saveItem(name = "치실", quantity = 0, nextReplacementDate = today.plusDays(2))

        notificationDispatchService.dispatch()

        val notification = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).single()
        assertEquals(NotificationType.LOW_STOCK, notification.type)
        assertEquals("여분이 부족해요", notification.title)
        assertEquals("치실 여분이 없어요. 지금 준비해두세요", notification.body)
    }

    @Test
    fun `미리보기는 합친 알림의 사유를 각각 센다`() {
        saveItem(name = "면도날", quantity = 0, nextReplacementDate = today.plusDays(3))

        val preview = notificationDispatchService.preview()

        assertEquals(1, preview.targetItemCount)
        assertEquals(
            mapOf(NotificationType.PRE_REPLACEMENT to 1, NotificationType.LOW_STOCK to 1),
            preview.countByType,
        )
    }

    @Test
    fun `발송 대상이 없으면 알림을 생성하지 않는다`() {
        saveItem(name = "칫솔", quantity = 2, nextReplacementDate = today.plusDays(10))

        notificationDispatchService.dispatch()

        assertEquals(0, notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size)
    }

    @Test
    fun `여분 부족 알림은 소모품 변경 후에도 발송 당시 정보를 반환한다`() {
        val item = saveItem("치실", 0, today.plusDays(2))
        notificationDispatchService.dispatch()
        item.nextReplacementDate = today.plusDays(30)
        item.quantity = 5
        itemRepository.flush()

        val response = notificationService.listAllNotification(userId).single()
        assertEquals("여분 부족", response.label)
        assertEquals(today.plusDays(2), response.nextReplacementDate)
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
        assertEquals(false, response.isBundled)
        assertTrue(response.cards.isEmpty())
        assertEquals("obrit://home", notificationService.markAsRead(userId, response.id).deepLink)
        assertFailsWith<ResourceNotFoundException> {
            notificationService.markAsRead(userId + 10000, requireNotNull(notification.id))
        }
    }
}
