package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.notification.entity.FcmToken
import depromeet.hotsix.obrit.notification.repository.FcmTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FcmTokenService(private val fcmTokenRepository: FcmTokenRepository) {
    @Transactional
    fun registerToken(userId: Long, token: String) {
        val existing = fcmTokenRepository.findByToken(token)

        if (existing == null) {
            fcmTokenRepository.save(FcmToken(userId = userId, token = token))
            return
        }

        existing.reassignOwner(userId)
    }
}
