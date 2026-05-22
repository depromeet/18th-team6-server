package depromeet.hotsix.obrit.user.service

import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.user.dto.RegisterUserRequest
import depromeet.hotsix.obrit.user.dto.RegisterUserResponse
import depromeet.hotsix.obrit.user.entity.User
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(private val userRepository: UserRepository) {

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

        val uuid = requireNotNull(request.uuid) { "UUID는 필수입니다." }

        val user = userRepository.findByUuid(uuid) ?: createUser(uuid)
        return RegisterUserResponse(id = requireNotNull(user.id), uuid = user.uuid)
    }

    private fun createUser(uuid: String): User = try {
        userRepository.save(User(uuid = uuid))
    } catch (e: DataIntegrityViolationException) {
        userRepository.findByUuid(uuid)
            ?: throw e
    }
}
