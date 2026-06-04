package depromeet.hotsix.obrit.notification.dto.response

import java.time.LocalDateTime

data class MarkReadNotificationResponse(val id: Long, val isRead: Boolean, val readAt: LocalDateTime)
