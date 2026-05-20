package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.notification.dto.response.RegisterFcmTokenResponse
import depromeet.hotsix.obrit.notification.entity.FcmToken
import depromeet.hotsix.obrit.notification.repository.FcmTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FcmTokenService(private val fcmTokenRepository: FcmTokenRepository) {
    @Transactional
    fun registerToken(userId: Long, token: String): RegisterFcmTokenResponse {
        val existing = fcmTokenRepository.findByToken(token)

        val fcmToken = if (existing == null) {
            fcmTokenRepository.save(FcmToken(userId = userId, token = token))
        } else {
            existing.reassignOwner(userId)
            existing
        }

        return RegisterFcmTokenResponse(
            id = fcmToken.id!!,
            userId = fcmToken.userId,
            token = fcmToken.token,
            createdAt = fcmToken.createdAt!!,
        )
    }
}
