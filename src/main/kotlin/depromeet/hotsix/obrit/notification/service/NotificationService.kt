package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.notification.dto.response.ListNotificationResponse
import depromeet.hotsix.obrit.notification.dto.response.MarkReadNotificationResponse
import depromeet.hotsix.obrit.notification.dto.response.NotificationCardResponse
import depromeet.hotsix.obrit.notification.entity.NotificationEntry
import depromeet.hotsix.obrit.notification.repository.NotificationEntryRepository
import depromeet.hotsix.obrit.notification.repository.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val notificationEntryRepository: NotificationEntryRepository,
    private val notificationDeepLinkService: NotificationDeepLinkService,
) {

    @Transactional(readOnly = true)
    fun listAllNotification(userId: Long): List<ListNotificationResponse> {
        val notifications = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
        if (notifications.isEmpty()) return emptyList()

        val entriesByNotificationId = notificationEntryRepository
            .findAllByNotificationIds(notifications.map { requireNotNull(it.id) })
            .groupBy { requireNotNull(it.notification.id) }

        return notifications.map {
            val cards = entriesByNotificationId[it.id].orEmpty().map(::toCardResponse)
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
                isBundled = cards.size > 1,
                cards = cards,
            )
        }
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

    private fun toCardResponse(entry: NotificationEntry) = NotificationCardResponse(
        entryId = requireNotNull(entry.id),
        type = entry.type,
        title = entry.title,
        content = entry.body,
        itemName = entry.itemName,
        label = entry.label,
        nextReplacementDate = entry.nextReplacementDate,
        itemId = entry.itemId,
        deepLink = notificationDeepLinkService.resolve(entry.itemId),
    )
}
