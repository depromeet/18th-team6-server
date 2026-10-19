package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.repository.ItemRepository
import depromeet.hotsix.obrit.notification.entity.NotificationSettings
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
        preReplacementStepDays: String = NotificationSettings.DEFAULT_PRE_REPLACEMENT_STEP_DAYS,
        preReplacementEnabled: Boolean = true,
        overdueEnabled: Boolean = true,
        lowStockEnabled: Boolean = true,
    ) = notificationSettingsService.update(
        preReplacementStepDays = preReplacementStepDays,
        lowStockLeadDays = NotificationSettings.DEFAULT_LOW_STOCK_LEAD_DAYS,
        overdueStepDays = NotificationSettings.DEFAULT_OVERDUE_STEP_DAYS,
        preReplacementEnabled = preReplacementEnabled,
        overdueEnabled = overdueEnabled,
        lowStockEnabled = lowStockEnabled,
    )

    private fun candidateFor(itemId: Long?) = notificationPolicyService.evaluate().find { it.itemId == itemId }

    private fun saveUserSettings(
        targetUserId: Long = userId,
        preReplacementEnabled: Boolean = true,
        overdueEnabled: Boolean = true,
        lowStockEnabled: Boolean = true,
    ) = userNotificationSettingsRepository.save(
        UserNotificationSettings(
            userId = targetUserId,
            preReplacementEnabled = preReplacementEnabled,
            overdueEnabled = overdueEnabled,
            lowStockEnabled = lowStockEnabled,
        ),
    )

    @Test
    fun `설정 행이 없는 사용자는 정책 알림 대상이 아니다`() {
        val preReplacement = saveItem(quantity = 2, nextReplacementDate = today.plusDays(3))
        val lowStock = saveItem(quantity = 0, nextReplacementDate = today.plusDays(1))
        val overdue = saveItem(quantity = 2, nextReplacementDate = today.minusDays(1))

        assertNull(candidateFor(preReplacement.id))
        assertNull(candidateFor(lowStock.id))
        assertNull(candidateFor(overdue.id))
    }

    @Test
    fun `유저가 세 유형을 모두 끄면 후보에서 제외된다`() {
        val item = saveItem(quantity = 2, nextReplacementDate = today.plusDays(3))
        val settings = saveUserSettings()
        assertEquals(listOf(NotificationType.PRE_REPLACEMENT), candidateFor(item.id)?.reasons)

        settings.preReplacementEnabled = false
        settings.overdueEnabled = false
        settings.lowStockEnabled = false
        userNotificationSettingsRepository.save(settings)

        assertNull(candidateFor(item.id))
    }

    @Test
    fun `유저가 여분 부족만 끄면 결합 후보에서 교체 예정 사유만 남는다`() {
        // 사전 스텝(D-3)과 여분 부족이 같은 날 성립한다.
        val item = saveItem(quantity = 0, nextReplacementDate = today.plusDays(3))
        val settings = saveUserSettings()
        assertEquals(
            listOf(NotificationType.PRE_REPLACEMENT, NotificationType.LOW_STOCK),
            candidateFor(item.id)?.reasons,
        )

        settings.lowStockEnabled = false
        userNotificationSettingsRepository.save(settings)

        assertEquals(listOf(NotificationType.PRE_REPLACEMENT), candidateFor(item.id)?.reasons)
    }

    @Test
    fun `유저가 교체 예정만 끄면 결합 후보에서 여분 부족 사유만 남는다`() {
        val item = saveItem(quantity = 0, nextReplacementDate = today.plusDays(3))
        saveUserSettings(preReplacementEnabled = false)

        assertEquals(listOf(NotificationType.LOW_STOCK), candidateFor(item.id)?.reasons)
    }

    @Test
    fun `유저가 교체 예정을 끄면 당일 알림도 받지 않는다`() {
        val item = saveItem(quantity = 2, nextReplacementDate = today)
        saveUserSettings(preReplacementEnabled = false)

        assertNull(candidateFor(item.id))
    }

    @Test
    fun `사전 스텝은 전역 설정을 따른다`() {
        val item = saveItem(quantity = 2, nextReplacementDate = today.plusDays(5))
        saveUserSettings()
        assertNull(candidateFor(item.id))

        updateGlobal(preReplacementStepDays = "5")

        assertEquals(listOf(NotificationType.PRE_REPLACEMENT), candidateFor(item.id)?.reasons)
    }

    @Test
    fun `전역에서 유형을 끄면 유저가 켜두었어도 제외된다`() {
        val item = saveItem(quantity = 2, nextReplacementDate = today.plusDays(3))
        saveUserSettings()

        updateGlobal(preReplacementEnabled = false)

        assertNull(candidateFor(item.id))
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

        saveUserSettings(preReplacementEnabled = false, overdueEnabled = false, lowStockEnabled = false)
        saveUserSettings(targetUserId = otherUserId)

        assertNull(candidateFor(item.id))
        assertEquals(listOf(NotificationType.PRE_REPLACEMENT), candidateFor(otherItem.id)?.reasons)
    }
}
