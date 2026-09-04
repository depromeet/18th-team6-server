package depromeet.hotsix.obrit.admin.service

import depromeet.hotsix.obrit.admin.dto.AdminNoticeForm
import depromeet.hotsix.obrit.admin.dto.AdminNotificationSettingsForm
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.repository.ItemRepository
import depromeet.hotsix.obrit.notification.DeviceRegistrationFixture
import depromeet.hotsix.obrit.notification.entity.NotificationType
import depromeet.hotsix.obrit.notification.repository.DeviceRegistrationRepository
import depromeet.hotsix.obrit.notification.repository.NotificationRepository
import depromeet.hotsix.obrit.notification.service.NotificationSettingsService
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
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminNotificationServiceTest {

    @Autowired
    private lateinit var adminNotificationService: AdminNotificationService

    @Autowired
    private lateinit var notificationSettingsService: NotificationSettingsService

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

    private fun registerDevice(targetUserId: Long = userId, fid: String = "fid-$targetUserId") {
        deviceRegistrationRepository.save(DeviceRegistrationFixture.deviceRegistration(targetUserId, fid))
    }

    private fun saveOverdueItem(name: String = "수건") = itemRepository.save(
        Item(
            userId = userId,
            categoryId = 1L,
            name = name,
            quantity = 2,
            replacementIntervalDays = 30,
            lastReplacedDate = today.minusDays(31),
            nextReplacementDate = today.minusDays(1),
        ),
    )

    @Test
    fun `기기 등록이 없으면 커버리지는 0이다`() {
        val coverage = adminNotificationService.getCoverage()

        assertEquals(0, coverage.registeredUserCount)
        assertEquals(0.0, coverage.coveragePercent)
    }

    @Test
    fun `기기를 등록하면 커버리지에 반영된다`() {
        registerDevice()

        val coverage = adminNotificationService.getCoverage()

        assertEquals(1, coverage.registeredUserCount)
        assertTrue(coverage.deviceCount >= 1)
    }

    @Test
    fun `미리보기는 발송하지 않고 대상만 집계한다`() {
        saveOverdueItem()

        val preview = adminNotificationService.getDashboard().preview

        assertEquals(1, preview.targetUserCount)
        assertEquals(1, preview.targetItemCount)
        assertEquals(1, preview.countByType[NotificationType.OVERDUE])
        assertEquals(0, notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size)
    }

    @Test
    fun `수동 실행은 자동 발송이 꺼져 있어도 동작한다`() {
        saveOverdueItem()
        notificationSettingsService.updateAutoDispatch(enabled = false)

        val count = adminNotificationService.dispatchNow()

        assertEquals(1, count)
        assertEquals(1, notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).size)
    }

    @Test
    fun `잘못된 정책 설정은 저장되지 않는다`() {
        assertFailsWith<BusinessException> {
            adminNotificationService.updateSettings(AdminNotificationSettingsForm(leadDays = 99))
        }

        assertEquals(3, notificationSettingsService.current().leadDays)
    }

    @Test
    fun `공지를 특정 사용자에게 발송하면 알림이 저장된다`() {
        registerDevice()

        val count = adminNotificationService.sendNotice(
            AdminNoticeForm(title = "알림 기능이 생겼어요", body = "교체 시기를 알려드릴게요", userId = userId),
        )

        assertEquals(1, count)
        val saved = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).single()
        assertEquals(NotificationType.NOTICE, saved.type)
        assertEquals("알림 기능이 생겼어요", saved.title)
    }

    @Test
    fun `기기가 등록되지 않은 사용자에게는 공지를 보내지 않는다`() {
        assertFailsWith<BusinessException> {
            adminNotificationService.sendNotice(AdminNoticeForm(title = "제목", body = "내용", userId = userId))
        }
    }

    @Test
    fun `전체 공지는 기기가 등록된 사용자에게만 발송된다`() {
        registerDevice()
        val withoutDevice = requireNotNull(userRepository.save(UserFixture.user(id = null)).id)

        val count = adminNotificationService.sendNotice(AdminNoticeForm(title = "제목", body = "내용"))

        assertEquals(1, count)
        assertEquals(0, notificationRepository.findAllByUserIdOrderByCreatedAtDesc(withoutDevice).size)
    }

    @Test
    fun `제목이 비어 있으면 공지를 발송하지 않는다`() {
        registerDevice()

        assertFailsWith<BusinessException> {
            adminNotificationService.sendNotice(AdminNoticeForm(title = "  ", body = "내용", userId = userId))
        }
    }
}
