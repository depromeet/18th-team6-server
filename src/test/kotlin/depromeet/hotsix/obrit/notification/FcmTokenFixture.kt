package depromeet.hotsix.obrit.notification

import depromeet.hotsix.obrit.notification.entity.FcmToken

object FcmTokenFixture {

    fun fcmToken(userId: Long = 1L, token: String = "test-fcm-token-abc123") = FcmToken(
        userId = userId,
        token = token,
    )
}
