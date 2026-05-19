package depromeet.hotsix.obrit.notification.repository

import depromeet.hotsix.obrit.notification.entity.FcmToken
import org.springframework.data.jpa.repository.JpaRepository

interface FcmTokenRepository : JpaRepository<FcmToken, Long> {
    fun findByToken(token: String): FcmToken?

    fun findAllByUserId(userId: Long): List<FcmToken>
}
