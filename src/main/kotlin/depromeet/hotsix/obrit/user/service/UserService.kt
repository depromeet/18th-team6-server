package depromeet.hotsix.obrit.user.service

import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(private val userRepository: UserRepository) {

    fun validateUserExist(userId: Long) {
        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException("User not found.")
        }
    }
}
