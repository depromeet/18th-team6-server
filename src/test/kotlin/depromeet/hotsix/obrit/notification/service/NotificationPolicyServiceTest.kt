package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.repository.ItemRepository
import depromeet.hotsix.obrit.notification.UserNotificationSettingsFixture
import depromeet.hotsix.obrit.notification.entity.NotificationSettings
import depromeet.hotsix.obrit.notification.entity.NotificationType
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
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationPolicyServiceTest {

    @Autowired
    private lateinit var notificationPolicyService: NotificationPolicyService

    @Autowired
    private lateinit var notificationSettingsService: NotificationSettingsService

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userNotificationSettingsRepository: UserNotificationSettingsRepository

    @Autowired
    private lateinit var clock: Clock

    private val today: LocalDate
        get() = LocalDate.now(clock)
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        userId = requireNotNull(userRepository.save(UserFixture.user(id = null)).id)
        userNotificationSettingsRepository.save(UserNotificationSettingsFixture.allEnabled(userId))
    }

    private fun saveItem(
        name: String,
        quantity: Int,
        nextReplacementDate: LocalDate,
        overdueNotifiedCount: Int = 0,
        lastOverdueNotifiedAt: LocalDate? = null,
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
            lastOverdueNotifiedAt = lastOverdueNotifiedAt,
            lowStockNotifiedAt = lowStockNotifiedAt,
        ),
    )

    private fun candidateFor(itemId: Long?) = notificationPolicyService.evaluate().find { it.itemId == itemId }

    private fun updateGlobal(
        overdueStepDays: String = NotificationSettings.DEFAULT_OVERDUE_STEP_DAYS,
        preReplacementEnabled: Boolean = true,
        overdueEnabled: Boolean = true,
        lowStockEnabled: Boolean = true,
    ) = notificationSettingsService.update(
        preReplacementStepDays = NotificationSettings.DEFAULT_PRE_REPLACEMENT_STEP_DAYS,
        lowStockLeadDays = NotificationSettings.DEFAULT_LOW_STOCK_LEAD_DAYS,
        overdueStepDays = overdueStepDays,
        preReplacementEnabled = preReplacementEnabled,
        overdueEnabled = overdueEnabled,
        lowStockEnabled = lowStockEnabled,
    )

    @Test
    fun `교체 D-7, D-3, D-1에는 교체 예정 사유 후보가 된다`() {
        listOf(7L, 3L, 1L).forEach { days ->
            val item = saveItem(name = "칫솔 D-$days", quantity = 2, nextReplacementDate = today.plusDays(days))

            val candidate = requireNotNull(candidateFor(item.id)) { "D-$days 후보가 없다" }
            assertEquals(listOf(NotificationType.PRE_REPLACEMENT), candidate.reasons)
            assertEquals("교체 D-$days", candidate.label())
        }
    }

    @Test
    fun `교체 예정일 당일에는 교체 예정 사유 후보가 되고 라벨은 D-day다`() {
        val item = saveItem(name = "수세미", quantity = 2, nextReplacementDate = today)

        val candidate = requireNotNull(candidateFor(item.id))
        assertEquals(listOf(NotificationType.PRE_REPLACEMENT), candidate.reasons)
        assertEquals("교체 D-day", candidate.label())
    }

    @Test
    fun `사전 스텝이 아닌 날에는 후보가 아니다`() {
        listOf(2L, 4L, 6L, 8L).forEach { days ->
            val item = saveItem(name = "칫솔 D-$days", quantity = 2, nextReplacementDate = today.plusDays(days))

            assertNull(candidateFor(item.id), "D-$days")
        }
    }

    @Test
    fun `여분이 0이고 기준 일수 이내면 여분 부족 사유 후보가 된다`() {
        val item = saveItem(name = "치실", quantity = 0, nextReplacementDate = today.plusDays(2))

        val candidate = requireNotNull(candidateFor(item.id))
        assertEquals(listOf(NotificationType.LOW_STOCK), candidate.reasons)
        assertEquals("여분 부족", candidate.label())
    }

    @Test
    fun `사전 스텝과 여분 부족이 같은 날 성립하면 한 후보에 두 사유를 합친다`() {
        val item = saveItem(name = "면도날", quantity = 0, nextReplacementDate = today.plusDays(3))

        val candidate = requireNotNull(candidateFor(item.id))
        assertEquals(listOf(NotificationType.PRE_REPLACEMENT, NotificationType.LOW_STOCK), candidate.reasons)
        assertEquals(NotificationType.PRE_REPLACEMENT, candidate.primaryType)
        assertEquals("교체 D-3 · 여분 부족", candidate.label())
    }

    @Test
    fun `이미 여분 부족 알림을 보낸 소모품은 재입고 전까지 다시 후보가 되지 않는다`() {
        val item = saveItem(
            name = "필터",
            quantity = 0,
            nextReplacementDate = today.plusDays(2),
            lowStockNotifiedAt = today.minusDays(1),
        )

        assertNull(candidateFor(item.id))
    }

    @Test
    fun `교체 예정일이 D+1 지나면 지연 사유 후보가 된다`() {
        val item = saveItem(name = "수건", quantity = 2, nextReplacementDate = today.minusDays(1))

        val candidate = requireNotNull(candidateFor(item.id))
        assertEquals(listOf(NotificationType.OVERDUE), candidate.reasons)
        assertEquals("교체 D+1", candidate.label())
    }

    @Test
    fun `D+1 지연 알림을 받은 소모품은 D+3에 다음 지연 알림 후보가 된다`() {
        val item = saveItem(
            name = "수건",
            quantity = 2,
            nextReplacementDate = today.minusDays(3),
            overdueNotifiedCount = 1,
            lastOverdueNotifiedAt = today.minusDays(2),
        )

        assertEquals(listOf(NotificationType.OVERDUE), candidateFor(item.id)?.reasons)
    }

    @Test
    fun `첫 스텝에 도달하기 전에는 지연 알림 후보가 아니다`() {
        updateGlobal(overdueStepDays = "5,10,15")
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

    @Test
    fun `지연 알림이 최소 간격에 막힌 날이어도 여분이 0이면 여분 부족 사유만 남는다`() {
        val item = saveItem(
            name = "수건",
            quantity = 0,
            nextReplacementDate = today.minusDays(5),
            overdueNotifiedCount = 1,
            lastOverdueNotifiedAt = today,
        )

        assertEquals(listOf(NotificationType.LOW_STOCK), candidateFor(item.id)?.reasons)
    }

    @Test
    fun `지연 알림 상한에 도달해도 여분이 0이면 여분 부족 사유만 남는다`() {
        val item = saveItem(
            name = "행주",
            quantity = 0,
            nextReplacementDate = today.minusDays(7),
            overdueNotifiedCount = 3,
        )

        assertEquals(listOf(NotificationType.LOW_STOCK), candidateFor(item.id)?.reasons)
    }

    @Test
    fun `지연 스텝과 여분 부족이 같은 날 성립하면 한 후보에 두 사유를 합친다`() {
        val item = saveItem(name = "수건", quantity = 0, nextReplacementDate = today.minusDays(1))

        val candidate = requireNotNull(candidateFor(item.id))
        assertEquals(listOf(NotificationType.OVERDUE, NotificationType.LOW_STOCK), candidate.reasons)
        assertEquals("교체 D+1 · 여분 부족", candidate.label())
    }

    @Test
    fun `전역에서 지연을 끄면 결합 후보에서 여분 부족 사유만 남는다`() {
        updateGlobal(overdueEnabled = false)
        val item = saveItem(name = "수건", quantity = 0, nextReplacementDate = today.minusDays(1))

        assertEquals(listOf(NotificationType.LOW_STOCK), candidateFor(item.id)?.reasons)
    }

    @Test
    fun `전역에서 여분 부족을 끄면 결합 후보에서 교체 예정 사유만 남는다`() {
        updateGlobal(lowStockEnabled = false)
        val item = saveItem(name = "면도날", quantity = 0, nextReplacementDate = today.plusDays(3))

        assertEquals(listOf(NotificationType.PRE_REPLACEMENT), candidateFor(item.id)?.reasons)
    }

    @Test
    fun `전역에서 교체 예정을 끄면 결합 후보에서 여분 부족 사유만 남는다`() {
        updateGlobal(preReplacementEnabled = false)
        val item = saveItem(name = "면도날", quantity = 0, nextReplacementDate = today.plusDays(3))

        assertEquals(listOf(NotificationType.LOW_STOCK), candidateFor(item.id)?.reasons)
    }

    @Test
    fun `성립한 사유가 모두 꺼져 있으면 후보가 아니다`() {
        updateGlobal(overdueEnabled = false)
        val item = saveItem(name = "수건", quantity = 2, nextReplacementDate = today.minusDays(1))

        assertNull(candidateFor(item.id))
    }
}
