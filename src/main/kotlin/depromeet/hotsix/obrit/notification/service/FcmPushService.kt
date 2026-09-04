package depromeet.hotsix.obrit.notification.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import depromeet.hotsix.obrit.notification.repository.DeviceRegistrationRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FcmPushService(private val deviceRegistrationRepository: DeviceRegistrationRepository) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @Transactional
    fun sendToUser(userId: Long, title: String, body: String) {
        val devices = deviceRegistrationRepository.findAllByUserId(userId)
        if (devices.isEmpty()) {
            log.warn("등록된 알림 기기가 없습니다. userId={}", userId)
            return
        }
        devices.forEach { sendToFid(it.fid, title, body) }
    }

    private fun sendToFid(fid: String, title: String, body: String) {
        try {
            FirebaseMessaging.getInstance().send(buildMessage(fid, title, body))
        } catch (e: FirebaseMessagingException) {
            handleFailure(fid, e)
        }
    }

    private fun buildMessage(fid: String, title: String, body: String): Message = Message.builder()
        .setFid(fid)
        .setNotification(
            Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build(),
        )
        .build()

    private fun handleFailure(fid: String, e: FirebaseMessagingException) {
        if (e.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
            log.info("만료된 기기 등록 삭제. fid={}", maskFid(fid))
            deviceRegistrationRepository.findByFid(fid)?.let { deviceRegistrationRepository.delete(it) }
        } else {
            log.error("FCM 전송 실패. fid={}, error={}", maskFid(fid), e.messagingErrorCode, e)
        }
    }

    private fun maskFid(fid: String): String = fid.take(6) + "***"
}
