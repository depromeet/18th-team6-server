package depromeet.hotsix.obrit.notification.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "알림 목록 항목")
data class ListNotificationResponse(
    @field:Schema(description = "알림 ID")
    val id: Long,

    @field:Schema(description = "알림 제목")
    val title: String,

    @field:Schema(description = "알림 내용")
    val content: String,

    @field:Schema(description = "읽음 여부")
    val isRead: Boolean,

    @field:Schema(description = "수신 일시")
    val createdAt: LocalDateTime,
)
