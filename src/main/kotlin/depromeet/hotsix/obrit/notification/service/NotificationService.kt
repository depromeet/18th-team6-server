package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.notification.dto.response.ListNotificationResponse
import depromeet.hotsix.obrit.notification.dto.response.MarkReadNotificationResponse
import depromeet.hotsix.obrit.notification.repository.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val notificationDeepLinkService: NotificationDeepLinkService,
) {

    @Transactional(readOnly = true)
    fun listAllNotification(userId: Long): List<ListNotificationResponse> =
        notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map {
            ListNotificationResponse(
                id = it.id!!,
                title = it.title,
                content = it.body,
                isRead = it.isRead,
                createdAt = it.createdAt!!,
                label = it.label,
                nextReplacementDate = it.nextReplacementDate,
                itemId = it.itemId,
                deepLink = notificationDeepLinkService.resolve(it.itemId),
            )
        }

    @Transactional
    fun markAsRead(userId: Long, notificationId: Long): MarkReadNotificationResponse {
        val notification = notificationRepository.findByIdAndUserId(notificationId, userId)
            ?: throw ResourceNotFoundException("존재하지 않는 알림입니다.")

        notification.markAsRead()

        return MarkReadNotificationResponse(
            id = notification.id!!,
            isRead = notification.isRead,
            readAt = notification.readAt!!,
            itemId = notification.itemId,
            deepLink = notificationDeepLinkService.resolve(notification.itemId),
        )
    }

    @Transactional
    fun markAsReadAll(userId: Long) {
        notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now())
    }
}
