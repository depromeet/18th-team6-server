package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.repository.ItemRepository
import depromeet.hotsix.obrit.notification.entity.NotificationType
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
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationPolicyServiceTest {

    @Autowired
    private lateinit var notificationPolicyService: NotificationPolicyService

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var clock: Clock

    private val today: LocalDate
        get() = LocalDate.now(clock)
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        userId = requireNotNull(userRepository.save(UserFixture.user(id = null)).id)
    }

    private fun saveItem(
        name: String,
        quantity: Int,
        nextReplacementDate: LocalDate,
        overdueNotifiedCount: Int = 0,
        lowStockNotifiedAt: LocalDate? = null,
    ): Item = itemRepository.save(
        Item(
            userId = userId,
            categoryId = 1L,
            name = name,
            quantity = quantity,
            replacementIntervalDays = 30,
            lastReplacedDate = nextReplacementDate.minusDays(30),
            nextReplacementDate = nextReplacementDate,
            overdueNotifiedCount = overdueNotifiedCount,
            lowStockNotifiedAt = lowStockNotifiedAt,
        ),
    )

    private fun candidateFor(itemId: Long?) = notificationPolicyService.evaluate().find { it.itemId == itemId }

    @Test
    fun `선행 일수 이내로 진입한 당일이면 사전 알림 후보가 된다`() {
        val item = saveItem(name = "칫솔", quantity = 2, nextReplacementDate = today.plusDays(3))

        assertEquals(NotificationType.PRE_REPLACEMENT, candidateFor(item.id)?.type)
    }

    @Test
    fun `선행 일수 이내가 아니면 사전 알림 후보가 아니다`() {
        val item = saveItem(name = "칫솔", quantity = 2, nextReplacementDate = today.plusDays(4))

        assertNull(candidateFor(item.id))
    }

    @Test
    fun `여분이 0이고 선행 일수 이내면 여분 부족 알림 후보가 된다`() {
        val item = saveItem(name = "치실", quantity = 0, nextReplacementDate = today.plusDays(1))

        assertEquals(NotificationType.LOW_STOCK, candidateFor(item.id)?.type)
    }

    @Test
    fun `사전 알림과 여분 부족 알림이 동시 성립하면 여분 부족 알림만 발송한다`() {
        val item = saveItem(name = "면도날", quantity = 0, nextReplacementDate = today.plusDays(3))

        assertEquals(NotificationType.LOW_STOCK, candidateFor(item.id)?.type)
    }

    @Test
    fun `이미 여분 부족 알림을 보낸 소모품은 재입고 전까지 다시 후보가 되지 않는다`() {
        val item = saveItem(
            name = "필터",
            quantity = 0,
            nextReplacementDate = today.plusDays(1),
            lowStockNotifiedAt = today.minusDays(1),
        )

        assertNull(candidateFor(item.id))
    }

    @Test
    fun `교체 예정일 당일에는 사전 알림도 지연 알림도 발송하지 않는다`() {
        val item = saveItem(name = "수세미", quantity = 2, nextReplacementDate = today)

        assertNull(candidateFor(item.id))
    }

    @Test
    fun `교체 예정일이 D+1 지나면 지연 알림 후보가 된다`() {
        val item = saveItem(name = "수건", quantity = 2, nextReplacementDate = today.minusDays(1))

        assertEquals(NotificationType.OVERDUE, candidateFor(item.id)?.type)
    }

    @Test
    fun `지연 알림 스텝에 해당하지 않는 날짜는 후보가 아니다`() {
        val item = saveItem(name = "수건", quantity = 2, nextReplacementDate = today.minusDays(2))

        assertNull(candidateFor(item.id))
    }

    @Test
    fun `지연 알림 상한에 도달하면 더 이상 후보가 되지 않는다`() {
        val item = saveItem(
            name = "행주",
            quantity = 2,
            nextReplacementDate = today.minusDays(7),
            overdueNotifiedCount = 3,
        )

        assertNull(candidateFor(item.id))
    }
}
