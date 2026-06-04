package depromeet.hotsix.obrit.notification.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class MarkReadNotificationResponse(
    @field:Schema(description = "알림 ID")
    val id: Long,
    @field:Schema(description = "읽음 여부")
    val isRead: Boolean,
    @field:Schema(description = "읽음 시각")
    val readAt: LocalDateTime,
)
