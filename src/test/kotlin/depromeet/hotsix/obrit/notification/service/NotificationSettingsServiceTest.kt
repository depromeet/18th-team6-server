package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.global.exception.BusinessException
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationSettingsServiceTest {

    @Autowired
    private lateinit var notificationSettingsService: NotificationSettingsService

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

    private fun update(
        leadDays: Int = 3,
        overdueStepDays: String = "1,4,7",
        preReplacementEnabled: Boolean = true,
        overdueEnabled: Boolean = true,
        lowStockEnabled: Boolean = true,
    ) = notificationSettingsService.update(
        leadDays = leadDays,
        overdueStepDays = overdueStepDays,
        preReplacementEnabled = preReplacementEnabled,
        overdueEnabled = overdueEnabled,
        lowStockEnabled = lowStockEnabled,
    )

    private fun candidateFor(itemId: Long?) = notificationPolicyService.evaluate().find { it.itemId == itemId }

    @Test
    fun `설정 행이 없으면 기본값으로 생성한다`() {
        val settings = notificationSettingsService.current()

        assertEquals(false, settings.autoDispatchEnabled)
        assertEquals(3, settings.leadDays)
        assertEquals(listOf(1, 4, 7), settings.overdueSteps())
    }

    @Test
    fun `선행 일수를 바꾸면 사전 알림 판정 기준이 함께 바뀐다`() {
        val item = saveItem(quantity = 2, nextReplacementDate = today.plusDays(5))
        assertNull(candidateFor(item.id))

        update(leadDays = 5)

        assertEquals(NotificationType.PRE_REPLACEMENT, candidateFor(item.id)?.type)
    }

    @Test
    fun `지연 스텝을 바꾸면 지연 알림 판정 기준이 함께 바뀐다`() {
        val item = saveItem(quantity = 2, nextReplacementDate = today.minusDays(2))
        assertNull(candidateFor(item.id))

        update(overdueStepDays = "2,5,10")

        assertEquals(NotificationType.OVERDUE, candidateFor(item.id)?.type)
    }

    @Test
    fun `유형을 끄면 해당 유형은 후보에서 제외된다`() {
        val item = saveItem(quantity = 0, nextReplacementDate = today.plusDays(1))
        assertEquals(NotificationType.LOW_STOCK, candidateFor(item.id)?.type)

        update(lowStockEnabled = false)

        assertNull(candidateFor(item.id))
    }

    @Test
    fun `선행 일수가 허용 범위를 벗어나면 예외를 던진다`() {
        assertFailsWith<BusinessException> { update(leadDays = 0) }
        assertFailsWith<BusinessException> { update(leadDays = 8) }
    }

    @Test
    fun `지연 스텝이 오름차순이 아니면 예외를 던진다`() {
        assertFailsWith<BusinessException> { update(overdueStepDays = "7,4,1") }
    }

    @Test
    fun `지연 스텝에 중복이 있으면 예외를 던진다`() {
        assertFailsWith<BusinessException> { update(overdueStepDays = "1,1,4") }
    }

    @Test
    fun `지연 스텝이 숫자가 아니면 예외를 던진다`() {
        assertFailsWith<BusinessException> { update(overdueStepDays = "1,넷,7") }
    }

    @Test
    fun `지연 스텝이 비어 있으면 예외를 던진다`() {
        assertFailsWith<BusinessException> { update(overdueStepDays = " ") }
    }

    @Test
    fun `지연 스텝 개수가 상한을 넘으면 예외를 던진다`() {
        assertFailsWith<BusinessException> { update(overdueStepDays = "1,2,3,4,5,6") }
    }

    @Test
    fun `지연 스텝 값이 허용 범위를 벗어나면 예외를 던진다`() {
        assertFailsWith<BusinessException> { update(overdueStepDays = "1,4,31") }
    }

    @Test
    fun `공백이 섞여 있어도 정규화해서 저장한다`() {
        val settings = update(overdueStepDays = " 2 , 6 , 9 ")

        assertEquals("2,6,9", settings.overdueStepDays)
    }

    @Test
    fun `저장된 스텝 값이 깨져 있어도 조회와 판정이 실패하지 않는다`() {
        saveItem(quantity = 2, nextReplacementDate = today.minusDays(1))
        notificationSettingsService.current().overdueStepDays = "1,깨진값,7"

        assertEquals(listOf(1, 7), notificationSettingsService.current().overdueSteps())
        assertEquals(NotificationType.OVERDUE, notificationPolicyService.evaluate().firstOrNull()?.type)
    }
}
