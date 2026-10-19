package depromeet.hotsix.obrit.receipt.client

import io.github.bucket4j.Bucket
import io.github.bucket4j.TimeMeter
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Gemini 호출 예산을 이중 토큰 버킷으로 제어한다.
 * - primaryBucket: 즉시 응답용. 분 단위로만 9개를 한 번에 보충(버스트 흡수).
 * - fallbackBucket: 속도 조절용. 그리디 보충으로 평균 ~10초당 1개(지속 처리량).
 *
 * 두 버킷 합산 분당 최대 15회로, Gemini의 슬라이딩 윈도우 RPM(15)을 넘지 않도록 한다.
 */
@Component
class GeminiRateLimiter(timeMeter: TimeMeter = TimeMeter.SYSTEM_MILLISECONDS) {

    private val primaryBucket: Bucket = Bucket.builder()
        .withCustomTimePrecision(timeMeter)
        .addLimit {
            it.capacity(PRIMARY_CAPACITY).refillIntervally(PRIMARY_CAPACITY, ONE_MINUTE).initialTokens(PRIMARY_CAPACITY)
        }
        .build()

    private val fallbackBucket: Bucket = Bucket.builder()
        .withCustomTimePrecision(timeMeter)
        .addLimit {
            it.capacity(FALLBACK_CAPACITY).refillGreedy(FALLBACK_CAPACITY, ONE_MINUTE).initialTokens(FALLBACK_CAPACITY)
        }
        .build()

    /** DB 조회 전 게이트. 두 버킷 중 하나라도 토큰이 있으면 true. */
    fun hasAvailableToken(): Boolean = primaryBucket.availableTokens > 0 || fallbackBucket.availableTokens > 0

    /** 실제 호출 직전 토큰 1개 소비. 즉시 응답용 버킷을 우선 사용한다. */
    fun tryConsume(): Boolean = primaryBucket.tryConsume(1) || fallbackBucket.tryConsume(1)

    companion object {
        private const val PRIMARY_CAPACITY = 9L
        private const val FALLBACK_CAPACITY = 6L
        private val ONE_MINUTE: Duration = Duration.ofMinutes(1)
    }
}
