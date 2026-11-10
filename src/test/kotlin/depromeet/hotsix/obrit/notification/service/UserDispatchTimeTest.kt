package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.repository.ItemRepository
import depromeet.hotsix.obrit.item.service.ItemService
import depromeet.hotsix.obrit.notification.DeviceRegistrationFixture
import depromeet.hotsix.obrit.notification.UserNotificationSettingsFixture
import depromeet.hotsix.obrit.notification.dto.request.UpdateNotificationSettingsRequest
import depromeet.hotsix.obrit.notification.entity.FcmSendResult
import depromeet.hotsix.obrit.notification.repository.DeviceRegistrationRepository
import depromeet.hotsix.obrit.notification.repository.NotificationEntryRepository
import depromeet.hotsix.obrit.notification.repository.NotificationRepository
import depromeet.hotsix.obrit.notification.repository.UserNotificationSettingsRepository
import depromeet.hotsix.obrit.user.entity.UserFixture
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserDispatchTimeTest {
    @Autowired
    private lateinit var policyService: NotificationPolicyService

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var notificationEntryRepository: NotificationEntryRepository

    @Autowired
    private lateinit var fcmPushService: FcmPushService

    @Autowired
    private lateinit var itemService: ItemService

    @Autowired
    private lateinit var deepLinkService: NotificationDeepLinkService

    @Autowired
    private lateinit var settingsService: UserNotificationSettingsService

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    private lateinit var settingsRepository: UserNotificationSettingsRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var deviceRepository: DeviceRegistrationRepository

    private val today = LocalDate.of(2026, 10, 19)
    private var userId = 0L

    @BeforeEach
    fun setUp() {
        userId = createUser()
    }

    private fun createUser(time: LocalTime = LocalTime.of(9, 0)): Long {
        val id = requireNotNull(userRepository.save(UserFixture.user(id = null)).id)
        settingsRepository.save(UserNotificationSettingsFixture.allEnabled(id).apply { dispatchTime = time })
        return id
    }

    private fun saveItem(owner: Long = userId, nextDate: LocalDate = today.plusDays(1)) = itemRepository.save(
        Item(
            userId = owner,
            categoryId = 1L,
            name = "칫솔",
            quantity = 2,
            replacementIntervalDays = 30,
            lastReplacedDate = nextDate.minusDays(30),
            nextReplacementDate = nextDate,
        ),
    )

    // 호출마다 새 서비스를 만들어, 메모리가 아닌 DB 완료일로 재실행 중복을 막는지도 검증한다.
    private fun dispatcher(instant: String, pushService: FcmPushService = fcmPushService) = NotificationDispatchService(
        notificationPolicyService = policyService,
        notificationRepository = notificationRepository,
        notificationEntryRepository = notificationEntryRepository,
        fcmPushService = pushService,
        itemService = itemService,
        clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC),
        notificationDeepLinkService = deepLinkService,
        userNotificationSettingsService = settingsService,
        transactionManager = transactionManager,
        scheduleZone = "Asia/Seoul",
    )

    private fun updateTime(time: LocalTime) = settingsService.updateSettings(
        userId,
        UpdateNotificationSettingsRequest(true, true, true, time),
    )

    private fun notificationCount(owner: Long = userId) =
        notificationRepository.findAllByUserIdOrderByCreatedAtDesc(owner).size

    @Test
    fun `기본 9시 전에는 발송하지 않고 9시에 발송한다`() {
        saveItem()
        dispatcher("2026-10-18T23:59:00Z").dispatchScheduled()
        assertEquals(0, notificationCount())
        assertNull(settingsRepository.findByUserId(userId)!!.lastDispatchDate)

        val result = dispatcher("2026-10-19T00:00:00Z").dispatchScheduled()
        assertEquals(1, notificationCount())
        assertEquals(1, result.skippedUserCount)
        assertEquals(today, settingsRepository.findByUserId(userId)!!.lastDispatchDate)
    }

    @Test
    fun `각 사용자의 저장된 시각과 30분 슬롯에 맞춰 발송한다`() {
        val eveningUser = createUser(LocalTime.of(18, 30))
        saveItem()
        saveItem(eveningUser)

        dispatcher("2026-10-19T00:00:00Z").dispatchScheduled()
        assertEquals(1, notificationCount())
        assertEquals(0, notificationCount(eveningUser))

        dispatcher("2026-10-19T09:29:00Z").dispatchScheduled()
        assertEquals(0, notificationCount(eveningUser))
        dispatcher("2026-10-19T09:30:00Z").dispatchScheduled()
        assertEquals(1, notificationCount(eveningUser))
        assertEquals(1, notificationCount())
    }

    @Test
    fun `미발송 사용자가 시간을 늦추면 바꾼 시각까지 기다린다`() {
        saveItem()
        updateTime(LocalTime.of(18, 30))

        dispatcher("2026-10-19T00:00:00Z").dispatchScheduled()
        assertEquals(0, notificationCount())
        dispatcher("2026-10-19T09:30:00Z").dispatchScheduled()
        assertEquals(1, notificationCount())
    }

    @Test
    fun `지난 시각으로 변경한 미발송 사용자는 다음 실행에서 발송한다`() {
        saveItem()
        dispatcher("2026-10-18T23:45:00Z").dispatchScheduled()
        updateTime(LocalTime.of(8, 30))

        dispatcher("2026-10-18T23:46:00Z").dispatchScheduled()
        assertEquals(1, notificationCount())
    }

    @Test
    fun `완료 후 반복 실행과 시간 변경은 중복 발송하지 않고 다음 날 새 시각에 발송한다`() {
        saveItem()
        dispatcher("2026-10-19T00:00:00Z").dispatchScheduled()
        updateTime(LocalTime.of(18, 30))
        assertEquals(today, settingsRepository.findByUserId(userId)!!.lastDispatchDate)

        dispatcher("2026-10-19T09:30:00Z").dispatchScheduled()
        dispatcher("2026-10-19T09:31:00Z").dispatchScheduled()
        assertEquals(1, notificationCount())

        dispatcher("2026-10-20T00:00:00Z").dispatchScheduled()
        assertEquals(1, notificationCount())
        dispatcher("2026-10-20T09:30:00Z").dispatchScheduled()
        assertEquals(2, notificationCount())
        assertEquals(today.plusDays(1), settingsRepository.findByUserId(userId)!!.lastDispatchDate)
    }

    @Test
    fun `같은 날 놓친 시각은 따라잡되 날짜가 바뀌면 어제 사전 알림은 보내지 않는다`() {
        saveItem(nextDate = today.plusDays(3))
        dispatcher("2026-10-19T05:12:00Z").dispatchScheduled()
        assertEquals(1, notificationCount())

        dispatcher("2026-10-20T05:12:00Z").dispatchScheduled()
        assertEquals(1, notificationCount())
    }

    @Test
    fun `UTC 자정과 관계없이 서울 자정과 23시 30분을 기준으로 판정한다`() {
        saveItem()
        updateTime(LocalTime.MIDNIGHT)
        dispatcher("2026-10-18T15:00:00Z").dispatchScheduled()
        assertEquals(today, settingsRepository.findByUserId(userId)!!.lastDispatchDate)

        val lateUser = createUser(LocalTime.of(23, 30))
        saveItem(lateUser)
        dispatcher("2026-10-19T14:29:00Z").dispatchScheduled()
        assertEquals(0, notificationCount(lateUser))
        dispatcher("2026-10-19T14:30:00Z").dispatchScheduled()
        assertEquals(1, notificationCount(lateUser))
        assertEquals(today, settingsRepository.findByUserId(lateUser)!!.lastDispatchDate)
    }

    @Test
    fun `대상이 없으면 완료하지 않고 같은 날 대상이 생기면 발송한다`() {
        dispatcher("2026-10-19T00:00:00Z").dispatchScheduled()
        assertNull(settingsRepository.findByUserId(userId)!!.lastDispatchDate)

        saveItem()
        dispatcher("2026-10-19T00:01:00Z").dispatchScheduled()
        assertEquals(1, notificationCount())
    }

    @Test
    fun `발송 실패 시 완료일을 남기지 않고 다음 실행에서 재시도한다`() {
        val item = saveItem(nextDate = today.minusDays(1))
        item.quantity = 0
        deviceRepository.save(DeviceRegistrationFixture.deviceRegistration(userId, "fid-$userId"))

        val first = dispatcher("2026-10-19T00:00:00Z").dispatchScheduled()
        val second = dispatcher("2026-10-19T00:01:00Z").dispatchScheduled()

        assertEquals(1, first.failedUserCount)
        assertEquals(1, second.failedUserCount)
        assertEquals(0, notificationCount())
        assertNull(settingsRepository.findByUserId(userId)!!.lastDispatchDate)
        assertNull(item.lowStockNotifiedAt)
        assertEquals(0, item.overdueNotifiedCount)
    }

    @Test
    fun `푸시 성공 후 완료일을 기록하고 실제 전송을 반복하지 않는다`() {
        val item = saveItem()
        val push = mock(FcmPushService::class.java)
        val data = mapOf("deepLink" to "obrit://items/${item.id}")
        `when`(push.sendToUser(userId, "교체 시기가 다가와요", "칫솔 교체가 얼마 남지 않았어요", data))
            .thenReturn(FcmSendResult.of(sentCount = 1, failedCount = 0))
        val service = dispatcher("2026-10-19T00:00:00Z", push)

        assertEquals(1, service.dispatchScheduled().sentUserCount)
        assertEquals(0, service.dispatchScheduled().sentUserCount)
        assertEquals(today, settingsRepository.findByUserId(userId)!!.lastDispatchDate)
        assertEquals(1, notificationCount())
        verify(push, times(1)).sendToUser(userId, "교체 시기가 다가와요", "칫솔 교체가 얼마 남지 않았어요", data)
    }

    @Test
    fun `수동 실행과 미리보기는 시각을 무시하고 당일 완료 사용자는 제외한다`() {
        saveItem()
        updateTime(LocalTime.of(18, 30))
        val service = dispatcher("2026-10-19T00:00:00Z")
        assertEquals(1, service.preview().targetUserCount)

        service.dispatch()
        assertEquals(0, service.preview().targetUserCount)
        service.dispatchScheduled()
        service.dispatch()
        assertEquals(1, notificationCount())
    }

    @Test
    fun `유형 토글이 모두 꺼지면 발송 시각이 지나도 완료하지 않는다`() {
        saveItem()
        settingsService.updateSettings(
            userId,
            UpdateNotificationSettingsRequest(false, false, false, LocalTime.of(9, 0)),
        )
        dispatcher("2026-10-19T00:00:00Z").dispatchScheduled()
        assertEquals(0, notificationCount())
        assertNull(settingsRepository.findByUserId(userId)!!.lastDispatchDate)
    }
}
