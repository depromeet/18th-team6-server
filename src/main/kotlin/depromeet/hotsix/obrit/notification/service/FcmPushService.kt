package depromeet.hotsix.obrit.notification.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import depromeet.hotsix.obrit.notification.entity.FcmSendResult
import depromeet.hotsix.obrit.notification.repository.DeviceRegistrationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * FCM 푸시 전송.
 *
 * 동기로 보내고 결과를 돌려준다. 비동기로 보내면 호출부가 결과를 볼 수 없어, 전송이 실패해도
 * 발송 이력과 알림 상태가 확정돼 재발송 기회가 사라진다. 배치 자체가 이미 별도 스레드에서 돌기 때문에
 * 여기서 비동기를 둘 이유가 없다.
 */
@Service
class FcmPushService(
    private val deviceRegistrationRepository: DeviceRegistrationRepository,
    private val firebaseStatusService: FirebaseStatusService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun sendToUser(userId: Long, title: String, body: String, data: Map<String, String> = emptyMap()): FcmSendResult {
        val devices = deviceRegistrationRepository.findAllByUserId(userId)
        if (devices.isEmpty()) {
            log.warn("등록된 알림 기기가 없습니다. userId={}", userId)
            return FcmSendResult.noDevice()
        }

        // 기기 없음과 초기화 실패를 구분해야 실패한 알림의 재시도 기회가 유지된다.
        if (!firebaseStatusService.canSend) {
            log.warn("Firebase가 초기화되지 않아 알림을 보내지 않습니다. state={}", firebaseStatusService.state)
            return FcmSendResult.of(sentCount = 0, failedCount = devices.size)
        }

        var sentCount = 0
        var failedCount = 0
        devices.forEach { device ->
            when (sendToFid(device.fid, title, body, data)) {
                SendOutcome.SENT -> sentCount++
                SendOutcome.FAILED -> failedCount++
                // 만료된 기기는 삭제했으므로 재시도 대상이 아니다.
                SendOutcome.UNREGISTERED -> Unit
            }
        }

        return FcmSendResult.of(sentCount = sentCount, failedCount = failedCount)
    }

    private fun sendToFid(fid: String, title: String, body: String, data: Map<String, String>): SendOutcome = try {
        FirebaseMessaging.getInstance().send(buildMessage(fid, title, body, data))
        SendOutcome.SENT
    } catch (e: FirebaseMessagingException) {
        handleFailure(fid, e)
    } catch (e: Exception) {
        // Firebase가 초기화되지 않았으면 getInstance()가 IllegalStateException을 던진다.
        // 동기 전송이라 예외가 호출부로 그대로 나가면 배치 전체가 중단되므로 여기서 실패로 바꾼다.
        log.error("FCM 전송 중 예상 못한 오류. fid={}", maskFid(fid), e)
        SendOutcome.FAILED
    }

    internal fun buildMessage(fid: String, title: String, body: String, data: Map<String, String>): Message =
        Message.builder()
            .setFid(fid)
            .putAllData(data)
            .setNotification(
                Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build(),
            )
            .build()

    private fun handleFailure(fid: String, e: FirebaseMessagingException): SendOutcome =
        if (e.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
            log.info("만료된 기기 등록 삭제. fid={}", maskFid(fid))
            deviceRegistrationRepository.findByFid(fid)?.let { deviceRegistrationRepository.delete(it) }
            SendOutcome.UNREGISTERED
        } else {
            log.error("FCM 전송 실패. fid={}, error={}", maskFid(fid), e.messagingErrorCode, e)
            SendOutcome.FAILED
        }

    private fun maskFid(fid: String): String = fid.take(6) + "***"

    private enum class SendOutcome { SENT, FAILED, UNREGISTERED }
}
