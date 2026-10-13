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
    @field:Schema(description = "대상 소모품 ID. 묶음·공지·기존 알림은 null")
    val itemId: Long?,
    @field:Schema(description = "읽음 처리 후 이동할 딥링크")
    val deepLink: String,
)
