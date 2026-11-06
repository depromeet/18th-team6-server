package depromeet.hotsix.obrit.receipt.client

import io.github.bucket4j.TimeMeter
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeminiRateLimiterTest {

    private class MutableTimeMeter(var nanos: Long = 0L) : TimeMeter {
        override fun currentTimeNanos(): Long = nanos
        override fun isWallClockBased(): Boolean = false

        fun advance(duration: Duration) {
            nanos += duration.toNanos()
        }
    }

    @Test
    fun `분당_15회까지_소비하고_16번째는_거부한다`() {
        val limiter = GeminiRateLimiter(MutableTimeMeter())

        repeat(15) { assertTrue(limiter.tryConsume(), "${it + 1}번째 소비는 성공해야 한다") }

        assertFalse(limiter.tryConsume(), "16번째 소비는 거부되어야 한다")
        assertFalse(limiter.hasAvailableToken())
    }

    @Test
    fun `1분이_지나면_토큰이_보충되어_다시_소비할_수_있다`() {
        val timeMeter = MutableTimeMeter()
        val limiter = GeminiRateLimiter(timeMeter)
        repeat(15) { limiter.tryConsume() }

        timeMeter.advance(Duration.ofMinutes(1))

        assertTrue(limiter.hasAvailableToken())
        assertTrue(limiter.tryConsume())
    }
}
