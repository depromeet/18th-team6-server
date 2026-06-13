package depromeet.hotsix.obrit.user.service

import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.global.exception.ConflictException
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.global.log.analytics.event.SignupCompletedDomainEvent
import depromeet.hotsix.obrit.user.dto.request.RegisterUserRequest
import depromeet.hotsix.obrit.user.dto.response.RegisterUserResponse
import depromeet.hotsix.obrit.user.entity.User
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val UUID_REGEX =
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()

@Service
@Transactional(readOnly = true)
class UserService(private val userRepository: UserRepository, private val eventPublisher: ApplicationEventPublisher) {
    fun validateUserExist(userId: Long) {
        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException("존재하지 않는 사용자입니다.")
        }
    }

    @Transactional
    fun registerOrGet(request: RegisterUserRequest): RegisterUserResponse {
        if (request.type != "uuid") {
            throw BusinessException("지원하지 않는 인증 수단입니다.")
        }

        if (!UUID_REGEX.matches(request.value)) {
            throw BusinessException("UUID 형식이 올바르지 않습니다.")
        }

        val existing = userRepository.findByUuidAndDeletedAtIsNull(request.value)
        if (existing != null) {
            return RegisterUserResponse(userId = requireNotNull(existing.id), uuid = requireNotNull(existing.uuid))
        }

        val user = createUser(request.value)
        eventPublisher.publishEvent(
            SignupCompletedDomainEvent(userId = requireNotNull(user.id), signupMethod = request.type),
        )

        return RegisterUserResponse(userId = requireNotNull(user.id), uuid = requireNotNull(user.uuid))
    }

    private fun createUser(uuid: String): User = try {
        userRepository.saveAndFlush(User(uuid = uuid))
    } catch (e: DataIntegrityViolationException) {
        throw ConflictException("이미 가입 처리 중입니다. 잠시 후 다시 시도해주세요.", e)
    }
}
