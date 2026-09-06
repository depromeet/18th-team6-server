package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.notification.entity.Notification
import depromeet.hotsix.obrit.notification.entity.NotificationType
import depromeet.hotsix.obrit.notification.repository.DeviceRegistrationRepository
import depromeet.hotsix.obrit.notification.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 정책 판정과 무관하게 운영자가 직접 내보내는 공지 발송.
 *
 * 기기 등록이 없는 사용자에게는 푸시가 닿지 않으므로 발송 대상은 등록 기기가 있는 사용자로 한정한다.
 */
@Service
class NotificationNoticeService(
    private val notificationRepository: NotificationRepository,
    private val deviceRegistrationRepository: DeviceRegistrationRepository,
    private val fcmPushService: FcmPushService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 등록 기기가 있는 모든 사용자에게 공지를 보낸다. 발송한 사용자 수를 반환한다. */
    @Transactional
    fun sendToAll(title: String, body: String): Int {
        validate(title, body)
        val userIds = deviceRegistrationRepository.findDistinctUserIds()
        if (userIds.isEmpty()) {
            throw BusinessException("등록된 기기가 없어 발송할 대상이 없습니다.")
        }

        log.info("공지 발송 시작. 대상 사용자 수={}", userIds.size)
        userIds.forEach { send(it, title, body) }
        return userIds.size
    }

    /** 특정 사용자에게만 공지를 보낸다. 테스트 발송과 CS 대응에 사용한다. */
    @Transactional
    fun sendToUser(userId: Long, title: String, body: String) {
        validate(title, body)
        if (deviceRegistrationRepository.findAllByUserId(userId).isEmpty()) {
            throw BusinessException("해당 사용자의 등록된 기기가 없습니다. userId=$userId")
        }
        send(userId, title, body)
    }

    private fun send(userId: Long, title: String, body: String) {
        notificationRepository.save(
            Notification(userId = userId, type = NotificationType.NOTICE, title = title, body = body),
        )
        fcmPushService.sendToUser(userId, title, body)
    }

    private fun validate(title: String, body: String) {
        if (title.isBlank()) throw BusinessException("공지 제목은 필수입니다.")
        if (body.isBlank()) throw BusinessException("공지 내용은 필수입니다.")
        if (title.length > MAX_TITLE_LENGTH) throw BusinessException("공지 제목은 ${MAX_TITLE_LENGTH}자 이하여야 합니다.")
        if (body.length > MAX_BODY_LENGTH) throw BusinessException("공지 내용은 ${MAX_BODY_LENGTH}자 이하여야 합니다.")
    }

    companion object {
        private const val MAX_TITLE_LENGTH = 100
        private const val MAX_BODY_LENGTH = 200
    }
}
