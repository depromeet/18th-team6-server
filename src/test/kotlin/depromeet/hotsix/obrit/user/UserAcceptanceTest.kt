package depromeet.hotsix.obrit.user

import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.user.dto.request.RegisterUserRequest
import depromeet.hotsix.obrit.user.repository.UserRepository
import depromeet.hotsix.obrit.user.service.UserService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserAcceptanceTest {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `신규_UUID로_회원_등록하면_회원이_생성된다`() {
        val request = RegisterUserRequest(type = "uuid", value = "550e8400-e29b-41d4-a716-446655440000")

        val response = userService.registerOrGet(request)

        assertNotNull(response.userId)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.uuid)
        assertEquals(1, userRepository.count())
    }

    @Test
    fun `이미_등록된_UUID로_요청하면_기존_회원을_반환한다`() {
        val request = RegisterUserRequest(type = "uuid", value = "550e8400-e29b-41d4-a716-446655440000")

        val first = userService.registerOrGet(request)
        val second = userService.registerOrGet(request)

        assertEquals(first.userId, second.userId)
        assertEquals(first.uuid, second.uuid)
        assertEquals(1, userRepository.count())
    }

    @Test
    fun `서로_다른_UUID는_각각_별도_회원이_생성된다`() {
        val request1 = RegisterUserRequest(type = "uuid", value = "00000000-0000-0000-0000-000000000001")
        val request2 = RegisterUserRequest(type = "uuid", value = "00000000-0000-0000-0000-000000000002")

        val response1 = userService.registerOrGet(request1)
        val response2 = userService.registerOrGet(request2)

        assertEquals(2, userRepository.count())
        assert(response1.userId != response2.userId)
    }

    @Test
    fun `지원하지_않는_인증_수단이면_예외가_발생한다`() {
        val request = RegisterUserRequest(type = "kakao", value = "some-token")

        val exception = assertThrows<BusinessException> {
            userService.registerOrGet(request)
        }
        assertEquals("지원하지 않는 인증 수단입니다.", exception.message)
    }

    @Test
    fun `UUID_형식이_올바르지_않으면_예외가_발생한다`() {
        val request = RegisterUserRequest(type = "uuid", value = "not-a-uuid")

        val exception = assertThrows<BusinessException> {
            userService.registerOrGet(request)
        }
        assertEquals("UUID 형식이 올바르지 않습니다.", exception.message)
    }

    @Test
    fun `삭제된_UUID로_다시_등록하면_새_회원이_생성된다`() {
        val request = RegisterUserRequest(type = "uuid", value = "550e8400-e29b-41d4-a716-446655440000")

        val first = userService.registerOrGet(request)
        val deleted = userRepository.findById(first.id).orElseThrow()
        deleted.softDelete()
        userRepository.save(deleted)

        val second = userService.registerOrGet(request)

        assert(first.id != second.id)
        assertEquals(2, userRepository.count())
    }
}
