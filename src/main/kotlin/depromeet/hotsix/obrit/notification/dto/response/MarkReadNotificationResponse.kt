package depromeet.hotsix.obrit.notification.dto.response

import java.util.Date

data class MarkReadNotificationResponse(val id: Long, val isRead: Boolean, val readAt: Date)
