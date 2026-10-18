package depromeet.hotsix.obrit.notification.controller

import depromeet.hotsix.obrit.notification.repository.UserNotificationSettingsRepository
import depromeet.hotsix.obrit.user.entity.UserFixture
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * v2.1에서 삭제한 알림 설정 필드(`enabled`, `leadDays`)를 구버전 앱이 계속 보내도 요청이 실패하지 않고,
 * 조회 응답에는 한동안 고정값이 남는지 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificationSettingsLegacyFieldTest {

    @LocalServerPort
    private var port: Int = 0

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userNotificationSettingsRepository: UserNotificationSettingsRepository

    private var userId: Long = 0L

    @BeforeEach
    fun setUp() {
        userId = requireNotNull(userRepository.save(UserFixture.user(id = null)).id)
    }

    @Test
    fun `저장 요청에 전체 토글과 선행 일수가 와도 무시하고 유형 토글만 저장한다`() {
        val response = send(
            "PUT",
            """
            {
              "enabled": false,
              "preReplacementEnabled": true,
              "overdueEnabled": false,
              "lowStockEnabled": true,
              "leadDays": 7,
              "dispatchTime": "23:00"
            }
            """.trimIndent(),
        )

        assertEquals(HttpStatus.OK.value(), response.statusCode(), response.body())
        val saved = requireNotNull(userNotificationSettingsRepository.findByUserId(userId))
        assertTrue(saved.preReplacementEnabled)
        assertFalse(saved.overdueEnabled)
        assertTrue(saved.lowStockEnabled)
        assertEquals(LocalTime.of(23, 0), saved.dispatchTime)
    }

    @Test
    fun `조회 응답에는 구버전 앱용 전체 토글과 선행 일수가 고정값으로 남는다`() {
        val response = send("GET", null)

        assertEquals(HttpStatus.OK.value(), response.statusCode(), response.body())
        val body = response.body()
        assertTrue(body.contains("\"enabled\":true"), body)
        assertTrue(body.contains("\"leadDays\":3"), body)
        assertTrue(body.contains("\"preReplacementEnabled\":false"), body)
        assertTrue(body.contains("\"overdueEnabled\":false"), body)
        assertTrue(body.contains("\"lowStockEnabled\":false"), body)
    }

    private fun send(method: String, body: String?): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/notifications/settings"))
            .header("Content-Type", "application/json")
            .header("X-User-Id", userId.toString())
            .method(
                method,
                body?.let { HttpRequest.BodyPublishers.ofString(it) } ?: HttpRequest.BodyPublishers.noBody(),
            )
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }
}
