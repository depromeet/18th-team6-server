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

/**
 * 여분 미입력(null)과 명시적 0의 구분을 검증한다.
 *
 * 온보딩만 마친 사용자는 여분을 입력하지 않는다. 이 값이 0으로 저장되면 전원이 여분 부족 알림 대상이 된다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SpareQuantityNullableTest {

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

    private fun saveItem(quantity: Int?, daysUntilReplacement: Long = 1): Item {
        val nextReplacementDate = today.plusDays(daysUntilReplacement)
        return itemRepository.save(
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
    }

    private fun candidateFor(itemId: Long?) = notificationPolicyService.evaluate().find { it.itemId == itemId }

    @Test
    fun `여분 미입력 아이템은 여분 부족 알림 후보가 되지 않는다`() {
        val item = saveItem(quantity = null)

        assertNull(candidateFor(item.id))
    }

    @Test
    fun `여분이 명시적 0이면 여분 부족 알림 후보가 된다`() {
        val item = saveItem(quantity = 0)

        assertEquals(NotificationType.LOW_STOCK, candidateFor(item.id)?.type)
    }

    @Test
    fun `여분이 있으면 여분 부족 알림 후보가 되지 않는다`() {
        val item = saveItem(quantity = 2)

        assertNull(candidateFor(item.id))
    }

    @Test
    fun `여분 미입력 아이템은 교체해도 미입력으로 남는다`() {
        val item = saveItem(quantity = null)

        item.replace(today)

        assertNull(item.quantity)
    }

    @Test
    fun `여분이 0인 아이템은 교체해도 0 아래로 내려가지 않는다`() {
        val item = saveItem(quantity = 0)

        item.replace(today)

        assertEquals(0, item.quantity)
    }

    @Test
    fun `여분을 미입력으로 되돌리면 여분 부족 알림을 다시 받을 수 있다`() {
        val item = saveItem(quantity = 0)
        item.recordLowStockNotification(today)

        item.updateSpareCount(null)

        assertNull(item.lowStockNotifiedAt)
    }
}
