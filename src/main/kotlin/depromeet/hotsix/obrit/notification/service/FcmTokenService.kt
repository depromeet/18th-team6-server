package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.notification.dto.response.RegisterFcmTokenResponse
import depromeet.hotsix.obrit.notification.entity.FcmToken
import depromeet.hotsix.obrit.notification.repository.FcmTokenRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FcmTokenService(private val fcmTokenRepository: FcmTokenRepository) {
    @Transactional(noRollbackFor = [DataIntegrityViolationException::class])
    fun registerToken(userId: Long, token: String): RegisterFcmTokenResponse {
        val fcmToken = findOrCreateToken(userId, token)
        return RegisterFcmTokenResponse(
            id = fcmToken.id!!,
            userId = fcmToken.userId,
            token = fcmToken.token,
            createdAt = fcmToken.createdAt!!,
        )
    }

    private fun findOrCreateToken(userId: Long, token: String): FcmToken {
        val existing = fcmTokenRepository.findByToken(token)
        if (existing != null) {
            existing.reassignOwner(userId)
            return existing
        }
        return try {
            fcmTokenRepository.saveAndFlush(FcmToken(userId = userId, token = token))
        } catch (e: DataIntegrityViolationException) {
            // 동시 요청으로 unique 제약 충돌 시 기존 토큰 소유자 재지정
            fcmTokenRepository.findByToken(token)!!.also { it.reassignOwner(userId) }
        }
    }
}
