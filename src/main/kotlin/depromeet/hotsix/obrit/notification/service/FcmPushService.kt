package depromeet.hotsix.obrit.notification.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import depromeet.hotsix.obrit.notification.repository.FcmTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class FcmPushService(private val fcmTokenRepository: FcmTokenRepository) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun sendToUser(userId: Long, title: String, body: String) {
        val tokens = fcmTokenRepository.findAllByUserId(userId)
        if (tokens.isEmpty()) {
            log.warn("FCM 토큰이 없습니다. userId={}", userId)
            return
        }
        tokens.forEach { sendToToken(it.token, title, body) }
    }

    private fun sendToToken(token: String, title: String, body: String) {
        val message = buildMessage(token, title, body)
        try {
            FirebaseMessaging.getInstance().send(message)
        } catch (e: FirebaseMessagingException) {
            handleFailure(token, e)
        }
    }

    private fun buildMessage(token: String, title: String, body: String): Message = Message.builder()
        .setToken(token)
        .setNotification(
            Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build(),
        )
        .build()

    private fun handleFailure(token: String, e: FirebaseMessagingException) {
        if (e.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
            log.info("만료된 FCM 토큰 삭제. token={}", token)
            fcmTokenRepository.findByToken(token)?.let { fcmTokenRepository.delete(it) }
        } else {
            log.error("FCM 전송 실패. token={}, error={}", token, e.messagingErrorCode, e)
        }
    }
}
