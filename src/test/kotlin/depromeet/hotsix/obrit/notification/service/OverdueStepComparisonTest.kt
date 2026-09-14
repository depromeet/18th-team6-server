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
 * 지연 알림 스텝 판정을 경과 기준(`>=`)으로 바꾼 뒤의 동작을 검증한다.
 *
 * 기본 스텝은 D+1, D+4, D+7이다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OverdueStepComparisonTest {

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

    /** 여분은 넉넉히 둬서 여분 부족 알림으로 내려가지 않게 한다. 지연 판정만 보기 위해서다. */
    private fun saveOverdueItem(daysOverdue: Long, notifiedCount: Int = 0, lastNotifiedAt: LocalDate? = null): Item {
        val nextReplacementDate = today.minusDays(daysOverdue)
        return itemRepository.save(
            Item(
                userId = userId,
                categoryId = 1L,
                name = "칫솔",
                quantity = 5,
                replacementIntervalDays = 30,
                lastReplacedDate = nextReplacementDate.minusDays(30),
                nextReplacementDate = nextReplacementDate,
                overdueNotifiedCount = notifiedCount,
                lastOverdueNotifiedAt = lastNotifiedAt,
            ),
        )
    }

    private fun candidateFor(itemId: Long?) = notificationPolicyService.evaluate().find { it.itemId == itemId }

    @Test
    fun `D+1 스텝을 놓친 아이템도 나중에 평가하면 후보가 된다`() {
        val item = saveOverdueItem(daysOverdue = 5)

        assertEquals(NotificationType.OVERDUE, candidateFor(item.id)?.type)
    }

    @Test
    fun `오래 방치된 아이템도 후보가 된다`() {
        val item = saveOverdueItem(daysOverdue = 100)

        assertEquals(NotificationType.OVERDUE, candidateFor(item.id)?.type)
    }

    @Test
    fun `교체 예정일 당일은 지연 후보가 아니다`() {
        val item = saveOverdueItem(daysOverdue = 0)

        assertNull(candidateFor(item.id))
    }

    @Test
    fun `방금 발송한 아이템은 다음 스텝 간격 전까지 후보가 아니다`() {
        // D+8, 첫 스텝을 어제 발송. 다음 스텝(D+4)까지 최소 간격은 3일이다.
        val item = saveOverdueItem(daysOverdue = 8, notifiedCount = 1, lastNotifiedAt = today.minusDays(1))

        assertNull(candidateFor(item.id))
    }

    @Test
    fun `최소 간격이 지나면 다음 스텝이 발송된다`() {
        val item = saveOverdueItem(daysOverdue = 8, notifiedCount = 1, lastNotifiedAt = today.minusDays(3))

        assertEquals(NotificationType.OVERDUE, candidateFor(item.id)?.type)
    }

    @Test
    fun `스텝을 모두 소진하면 더 이상 후보가 아니다`() {
        val item = saveOverdueItem(daysOverdue = 100, notifiedCount = 3, lastNotifiedAt = today.minusDays(30))

        assertNull(candidateFor(item.id))
    }

    @Test
    fun `교체를 완료하면 카운트가 초기화되어 다시 첫 스텝부터 시작한다`() {
        val item = saveOverdueItem(daysOverdue = 100, notifiedCount = 3, lastNotifiedAt = today.minusDays(30))

        item.replace(today.minusDays(31))

        assertEquals(0, item.overdueNotifiedCount)
        assertNull(item.lastOverdueNotifiedAt)
        assertEquals(NotificationType.OVERDUE, candidateFor(item.id)?.type)
    }

    @Test
    fun `주기를 편집하면 카운트가 초기화된다`() {
        val item = saveOverdueItem(daysOverdue = 100, notifiedCount = 3, lastNotifiedAt = today.minusDays(30))

        item.update(name = null, quantity = null, replacementIntervalDays = 40, lastReplacedDate = null)

        assertEquals(0, item.overdueNotifiedCount)
        assertNull(item.lastOverdueNotifiedAt)
    }
}
