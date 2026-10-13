package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.repository.ItemRepository
import depromeet.hotsix.obrit.notification.entity.NotificationType
import depromeet.hotsix.obrit.notification.entity.UserNotificationSettings
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
class NotificationPolicyUserSettingsTest {

    @Autowired
    private lateinit var notificationPolicyService: NotificationPolicyService

    @Autowired
    private lateinit var notificationSettingsService: NotificationSettingsService

    @Autowired
    private lateinit var userNotificationSettingsRepository: UserNotificationSettingsRepository

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

    private fun saveItem(quantity: Int, nextReplacementDate: LocalDate): Item = itemRepository.save(
        Item(
            userId = userId,
            categoryId = 1L,
            name = "칫솔",
            quantity = quantity,
            replacementIntervalDays = 30,
            lastReplacedDate = nextReplacementDate.minusDays(30),
            nextReplacementDate = nextReplacementDate,
        ),
    )

    private fun updateGlobal(
        preReplacementEnabled: Boolean = true,
        overdueEnabled: Boolean = true,
        lowStockEnabled: Boolean = true,
    ) = notificationSettingsService.update(
        leadDays = 3,
        overdueStepDays = "1,4,7",
        preReplacementEnabled = preReplacementEnabled,
        overdueEnabled = overdueEnabled,
        lowStockEnabled = lowStockEnabled,
    )

    private fun candidateFor(itemId: Long?) = notificationPolicyService.evaluate().find { it.itemId == itemId }

    @Test
    fun `유저가 알림을 끄면 후보에서 제외된다`() {
        val item = saveItem(quantity = 2, nextReplacementDate = today.plusDays(3))
        assertEquals(NotificationType.PRE_REPLACEMENT, candidateFor(item.id)?.type)

        userNotificationSettingsRepository.save(UserNotificationSettings(userId = userId, enabled = false))

        assertNull(candidateFor(item.id))
    }

    @Test
    fun `유저가 유형을 끄면 해당 유형만 제외된다`() {
        // 여분 부족과 사전 알림이 동시에 성립하는 날짜. 우선순위상 여분 부족이 먼저다.
        val item = saveItem(quantity = 0, nextReplacementDate = today.plusDays(3))
        assertEquals(NotificationType.LOW_STOCK, candidateFor(item.id)?.type)

        userNotificationSettingsRepository.save(
            UserNotificationSettings(userId = userId, lowStockEnabled = false),
        )

        assertEquals(NotificationType.PRE_REPLACEMENT, candidateFor(item.id)?.type)
    }

    @Test
    fun `유저 선행 일수가 전역 값보다 우선한다`() {
        val item = saveItem(quantity = 2, nextReplacementDate = today.plusDays(5))
        assertNull(candidateFor(item.id))

        userNotificationSettingsRepository.save(UserNotificationSettings(userId = userId, leadDays = 5))

        assertEquals(NotificationType.PRE_REPLACEMENT, candidateFor(item.id)?.type)
    }

    @Test
    fun `전역에서 유형을 끄면 유저가 켜두었어도 제외된다`() {
        val item = saveItem(quantity = 2, nextReplacementDate = today.plusDays(3))
        userNotificationSettingsRepository.save(
            UserNotificationSettings(userId = userId, preReplacementEnabled = true),
        )

        updateGlobal(preReplacementEnabled = false)

        assertNull(candidateFor(item.id))
    }

    @Test
    fun `유저 설정이 없는 사용자는 전역 기본값으로 판정된다`() {
        val item = saveItem(quantity = 2, nextReplacementDate = today.plusDays(5))
        assertNull(candidateFor(item.id))

        notificationSettingsService.update(
            leadDays = 5,
            overdueStepDays = "1,4,7",
            preReplacementEnabled = true,
            overdueEnabled = true,
            lowStockEnabled = true,
        )

        assertEquals(NotificationType.PRE_REPLACEMENT, candidateFor(item.id)?.type)
    }

    @Test
    fun `한 유저의 설정이 다른 유저의 판정에 영향을 주지 않는다`() {
        val otherUserId = requireNotNull(userRepository.save(UserFixture.user(id = null)).id)
        val item = saveItem(quantity = 2, nextReplacementDate = today.plusDays(3))
        val otherItem = itemRepository.save(
            Item(
                userId = otherUserId,
                categoryId = 1L,
                name = "면도날",
                quantity = 2,
                replacementIntervalDays = 30,
                lastReplacedDate = today.minusDays(27),
                nextReplacementDate = today.plusDays(3),
            ),
        )

        userNotificationSettingsRepository.save(UserNotificationSettings(userId = userId, enabled = false))

        assertNull(candidateFor(item.id))
        assertEquals(NotificationType.PRE_REPLACEMENT, candidateFor(otherItem.id)?.type)
    }
}
