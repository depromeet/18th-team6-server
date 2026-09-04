package depromeet.hotsix.obrit.notification.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
class NotificationSchedulerServiceTest {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `스케줄러는 기본적으로 비활성화되어 자동 발송되지 않는다`() {
        assertEquals(0, applicationContext.getBeanNamesForType(NotificationSchedulerService::class.java).size)
    }

    @Test
    fun `수동 실행용 발송 서비스는 스케줄러와 무관하게 항상 등록된다`() {
        assertEquals(1, applicationContext.getBeanNamesForType(NotificationDispatchService::class.java).size)
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = ["notification.schedule.enabled=true"])
    class EnabledTest {

        @Autowired
        private lateinit var applicationContext: ApplicationContext

        @Test
        fun `스위치를 켜면 스케줄러가 등록된다`() {
            assertEquals(1, applicationContext.getBeanNamesForType(NotificationSchedulerService::class.java).size)
        }
    }
}
