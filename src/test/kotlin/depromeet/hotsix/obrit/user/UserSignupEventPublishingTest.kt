package depromeet.hotsix.obrit.user

import depromeet.hotsix.obrit.global.log.analytics.repository.AnalyticsEventRepository
import depromeet.hotsix.obrit.user.dto.request.RegisterUserRequest
import depromeet.hotsix.obrit.user.repository.UserRepository
import depromeet.hotsix.obrit.user.service.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals

// AFTER_COMMIT 리스너는 트랜잭션 커밋 후 실행되므로 @Transactional 롤백 환경에서는 호출되지 않는다.
// 이 테스트는 의도적으로 실커밋 환경에서 동작하며, 사용/이벤트 데이터를 @BeforeEach/@AfterEach로 직접 정리한다.
@SpringBootTest
@ActiveProfiles("test")
class UserSignupEventPublishingTest {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var analyticsEventRepository: AnalyticsEventRepository

    @BeforeEach
    fun cleanup() {
        analyticsEventRepository.deleteAll()
        userRepository.deleteAll()
    }

    @AfterEach
    fun teardown() {
        analyticsEventRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `신규_회원_가입_시_signup_completed_이벤트가_발행된다`() {
        val request = RegisterUserRequest(type = "uuid", value = "550e8400-e29b-41d4-a716-446655440000")

        val response = userService.registerOrGet(request)

        val events = analyticsEventRepository.findAll()
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("signup_completed", event.eventName)
        assertEquals(response.userId, event.userId)
        assert(event.properties.contains("signup_method")) { "properties=${event.properties}" }
        assert(event.properties.contains("uuid")) { "properties=${event.properties}" }
    }

    @Test
    fun `이미_등록된_UUID로_재요청해도_signup_completed_이벤트는_추가되지_않는다`() {
        val request = RegisterUserRequest(type = "uuid", value = "550e8400-e29b-41d4-a716-446655440000")

        userService.registerOrGet(request)
        userService.registerOrGet(request)

        assertEquals(1, analyticsEventRepository.count())
    }
}
