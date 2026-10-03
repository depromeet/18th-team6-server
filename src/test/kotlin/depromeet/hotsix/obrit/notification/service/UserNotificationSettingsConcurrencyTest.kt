package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.notification.dto.request.ReportNotificationPermissionRequest
import depromeet.hotsix.obrit.notification.dto.request.UpdateNotificationSettingsRequest
import depromeet.hotsix.obrit.notification.entity.NotificationPermissionStatus
import depromeet.hotsix.obrit.notification.repository.UserNotificationSettingsRepository
import depromeet.hotsix.obrit.user.entity.UserFixture
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class UserNotificationSettingsConcurrencyTest {
    @Autowired
    private lateinit var settingsService: UserNotificationSettingsService

    @Autowired
    private lateinit var settingsRepository: UserNotificationSettingsRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `동시에 최초 설정을 저장해도 모든 요청이 성공하고 한 행만 생성된다`() {
        val userId = requireNotNull(userRepository.saveAndFlush(UserFixture.user(id = null)).id)
        val executor = Executors.newFixedThreadPool(4)
        val ready = CountDownLatch(4)
        val start = CountDownLatch(1)
        try {
            val requests = (1..4).map { days ->
                executor.submit {
                    ready.countDown()
                    check(start.await(10, TimeUnit.SECONDS))
                    settingsService.updateSettings(
                        userId,
                        UpdateNotificationSettingsRequest(true, true, true, true, days, LocalTime.of(9, 0)),
                    )
                }
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS))
            start.countDown()
            requests.forEach { it.get(20, TimeUnit.SECONDS) }

            val rows = settingsRepository.findByUserIdIn(listOf(userId))
            assertEquals(1, rows.size)
            assertTrue(rows.single().leadDays in 1..4)
        } finally {
            start.countDown()
            executor.shutdownNow()
            executor.awaitTermination(10, TimeUnit.SECONDS)
            settingsRepository.findByUserId(userId)?.let { settingsRepository.delete(it) }
            userRepository.deleteById(userId)
        }
    }

    @Test
    fun `최초 설정 저장과 권한 보고가 동시에 와도 두 값 모두 보존된다`() {
        val userId = requireNotNull(userRepository.saveAndFlush(UserFixture.user(id = null)).id)
        val executor = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        try {
            val update = executor.submit {
                ready.countDown()
                check(start.await(10, TimeUnit.SECONDS))
                settingsService.updateSettings(
                    userId,
                    UpdateNotificationSettingsRequest(false, true, false, true, 7, LocalTime.of(20, 30)),
                )
            }
            val permission = executor.submit {
                ready.countDown()
                check(start.await(10, TimeUnit.SECONDS))
                settingsService.reportPermission(
                    userId,
                    ReportNotificationPermissionRequest(NotificationPermissionStatus.GRANTED),
                )
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS))
            start.countDown()
            update.get(20, TimeUnit.SECONDS)
            permission.get(20, TimeUnit.SECONDS)

            val settings = settingsRepository.findByUserIdIn(listOf(userId)).single()
            assertEquals(false, settings.enabled)
            assertEquals(false, settings.overdueEnabled)
            assertEquals(7, settings.leadDays)
            assertEquals(LocalTime.of(20, 30), settings.dispatchTime)
            assertEquals(NotificationPermissionStatus.GRANTED, settings.permissionStatus)
        } finally {
            start.countDown()
            executor.shutdownNow()
            executor.awaitTermination(10, TimeUnit.SECONDS)
            settingsRepository.findByUserId(userId)?.let { settingsRepository.delete(it) }
            userRepository.deleteById(userId)
        }
    }
}
