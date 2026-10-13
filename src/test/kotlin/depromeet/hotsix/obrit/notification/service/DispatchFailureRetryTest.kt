package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.repository.ItemRepository
import depromeet.hotsix.obrit.notification.DeviceRegistrationFixture
import depromeet.hotsix.obrit.notification.repository.DeviceRegistrationRepository
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
import kotlin.test.assertNull

/**
 * 전송 실패가 알림 상태를 확정하지 않는지 검증한다.
 *
 * 테스트 환경에는 Firebase가 초기화돼 있지 않아, 기기가 등록된 사용자에게 보내면 전송이 실패한다.
 * 실제 전송 실패와 같은 경로다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DispatchFailureRetryTest {

    @Autowired
    private lateinit var notificationDispatchService: NotificationDispatchService

    @Autowired
    private lateinit var deviceRegistrationRepository: DeviceRegistrationRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

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

    private fun saveLowStockItem(): Item {
        val nextReplacementDate = today.plusDays(1)
        return itemRepository.save(
            Item(
                userId = userId,
                categoryId = 1L,
                name = "칫솔",
                quantity = 0,
                replacementIntervalDays = 30,
                lastReplacedDate = nextReplacementDate.minusDays(30),
                nextReplacementDate = nextReplacementDate,
            ),
        )
    }

    @Test
    fun `전송에 실패하면 실패로 집계된다`() {
        saveLowStockItem()
        deviceRegistrationRepository.save(DeviceRegistrationFixture.deviceRegistration(userId, "fid-$userId"))

        val result = notificationDispatchService.dispatch()

        assertEquals(1, result.failedUserCount)
        assertEquals(0, result.sentUserCount)
    }

    @Test
    fun `전송에 실패하면 여분 부족 기록이 남지 않는다`() {
        val item = saveLowStockItem()
        deviceRegistrationRepository.save(DeviceRegistrationFixture.deviceRegistration(userId, "fid-$userId"))

        notificationDispatchService.dispatch()

        assertNull(requireNotNull(itemRepository.findById(item.id!!).orElse(null)).lowStockNotifiedAt)
    }

    @Test
    fun `전송에 실패하면 발송 이력이 남지 않아 다음 배치에서 다시 시도한다`() {
        saveLowStockItem()
        deviceRegistrationRepository.save(DeviceRegistrationFixture.deviceRegistration(userId, "fid-$userId"))

        notificationDispatchService.dispatch()
        val second = notificationDispatchService.dispatch()

        assertEquals(0, notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size)
        assertEquals(1, second.failedUserCount)
    }

    @Test
    fun `기기가 없는 사용자는 실패가 아니라 대상 제외로 집계된다`() {
        saveLowStockItem()

        val result = notificationDispatchService.dispatch()

        assertEquals(1, result.skippedUserCount)
        assertEquals(0, result.failedUserCount)
    }
}
