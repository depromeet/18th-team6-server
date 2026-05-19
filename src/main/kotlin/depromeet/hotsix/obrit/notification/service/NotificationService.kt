package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.global.exception.ConflictException
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.notification.dto.response.ListNotificationResponse
import depromeet.hotsix.obrit.notification.repository.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(private val notificationRepository: NotificationRepository) {
    fun listAllNotification(userId: Long): List<ListNotificationResponse> =
        notificationRepository.findAllByUserId(userId).map {
            ListNotificationResponse(
                id = it.id!!,
                title = it.title,
                content = it.body,
                isRead = it.isRead,
            )
        }

    @Transactional
    fun sendNotification(userId: Long) {
        // TODO: 알림 전송 PRD 구현 시 작성
    }

    @Transactional
    fun markAsRead(userId: Long, notificationId: Long) {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { ResourceNotFoundException("존재하지 않는 알림입니다.") }

        if (!notification.isOwnedBy(userId)) {
            throw ConflictException("알림을 읽을 권한이 없습니다.")
        }

        notification.markAsRead()
    }

    @Transactional
    fun markAsReadAll(userId: Long) {
        notificationRepository.findAllByUserId(userId)
            .filter { !it.isRead }
            .forEach { it.markAsRead() }
    }
}
