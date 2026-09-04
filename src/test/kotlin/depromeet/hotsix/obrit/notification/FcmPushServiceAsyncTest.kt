package depromeet.hotsix.obrit.notification

import depromeet.hotsix.obrit.notification.service.FcmPushService
import org.junit.jupiter.api.Test
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
    fun `sendToUser는_비동기_스레드풀에서_실행된다`() {
        // Given: 등록 기기가 없는 userId → Firebase 호출 없이 종료
        val executor = taskExecutor as ThreadPoolTaskExecutor
        val beforeCount = executor.threadPoolExecutor.completedTaskCount

        // When
        fcmPushService.sendToUser(99999L, "test", "test")

        // Then: 동기 실행이면 스레드풀 작업 수가 끝까지 늘지 않는다
        assertTrue(
            awaitCompletedTaskIncrease(executor, beforeCount),
            "비동기 스레드풀에서 작업이 실행되어야 합니다",
        )
    }

    private fun awaitCompletedTaskIncrease(
        executor: ThreadPoolTaskExecutor,
        beforeCount: Long,
        timeoutMillis: Long = 5_000,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (executor.threadPoolExecutor.completedTaskCount > beforeCount) return true
            Thread.sleep(20)
        }
        return false
    }
}
