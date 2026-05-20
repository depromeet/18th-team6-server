package depromeet.hotsix.obrit.notification

import depromeet.hotsix.obrit.notification.service.FcmPushService
import org.junit.jupiter.api.Test
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.Executor
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class FcmPushServiceAsyncTest {

    @Autowired
    private lateinit var fcmPushService: FcmPushService

    @Autowired
    private lateinit var taskExecutor: Executor

    @Test
    fun `FcmPushService는_Async_프록시가_적용되어_있다`() {
        assertTrue(AopUtils.isAopProxy(fcmPushService), "FcmPushService에 @Async 프록시가 적용되어야 합니다")
    }

    @Test
    fun `sendToUser는_비동기_스레드풀에서_실행된다`() {
        // Given: 존재하지 않는 userId → 토큰 없음 → Firebase 호출 없이 종료
        val executor = taskExecutor as ThreadPoolTaskExecutor
        val beforeCount = executor.threadPoolExecutor.completedTaskCount

        // When
        fcmPushService.sendToUser(99999L, "test", "test")
        Thread.sleep(500)

        // Then: 비동기 스레드풀에서 작업이 실행됨
        val afterCount = executor.threadPoolExecutor.completedTaskCount
        assertTrue(afterCount > beforeCount, "비동기 스레드풀에서 작업이 실행되어야 합니다")
    }
}
