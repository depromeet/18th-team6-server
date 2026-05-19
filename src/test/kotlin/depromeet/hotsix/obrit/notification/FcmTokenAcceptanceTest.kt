package depromeet.hotsix.obrit.notification

import depromeet.hotsix.obrit.notification.repository.FcmTokenRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FcmTokenAcceptanceTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var fcmTokenRepository: FcmTokenRepository

    @BeforeEach
    fun setUp() {
        fcmTokenRepository.deleteAllInBatch()
    }

    @Test
    fun `FCM_토큰을_등록하면_DB에_저장된다`() {
        // When
        `FCM 토큰을 등록한다`(userId = 1L, token = "device-token-abc")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        // Then
        val tokens = fcmTokenRepository.findAllByUserId(1L)
        assertEquals(1, tokens.size)
        assertEquals("device-token-abc", tokens[0].token)
    }

    @Test
    fun `동일_토큰을_재등록하면_중복_저장되지_않는다`() {
        // Given
        `FCM 토큰을 등록한다`(userId = 1L, token = "same-token")

        // When
        `FCM 토큰을 등록한다`(userId = 1L, token = "same-token")

        // Then
        val allTokens = fcmTokenRepository.findAllByUserId(1L)
        assertEquals(1, allTokens.size)
    }

    @Test
    fun `같은_사용자가_다른_토큰을_등록하면_멀티_디바이스로_저장된다`() {
        // Given
        `FCM 토큰을 등록한다`(userId = 1L, token = "device-a-token")

        // When
        `FCM 토큰을 등록한다`(userId = 1L, token = "device-b-token")

        // Then
        val tokens = fcmTokenRepository.findAllByUserId(1L)
        assertEquals(2, tokens.size)
    }

    @Test
    fun `다른_사용자가_동일_토큰을_등록하면_소유자가_변경된다`() {
        // Given: 유저1이 토큰 등록
        `FCM 토큰을 등록한다`(userId = 1L, token = "shared-device-token")

        // When: 유저2가 같은 토큰 등록 (디바이스 주인 변경)
        `FCM 토큰을 등록한다`(userId = 2L, token = "shared-device-token")

        // Then: 유저1은 토큰 없음, 유저2가 소유
        val user1Tokens = fcmTokenRepository.findAllByUserId(1L)
        val user2Tokens = fcmTokenRepository.findAllByUserId(2L)
        assertTrue(user1Tokens.isEmpty())
        assertEquals(1, user2Tokens.size)
        assertEquals("shared-device-token", user2Tokens[0].token)
    }

    @Test
    fun `빈_토큰으로_등록하면_400을_반환한다`() {
        // When & Then
        `FCM 토큰을 등록한다`(userId = 1L, token = "")
            .andExpect(status().isBadRequest)
    }

    private fun `FCM 토큰을 등록한다`(userId: Long, token: String) = mockMvc.perform(
        post("/fcm-tokens")
            .header("X-User-Id", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"token": "$token"}"""),
    )
}
