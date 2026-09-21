package depromeet.hotsix.obrit.notification.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
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

    @field:Schema(description = "발송 당시 라벨. 묶음·공지·기존 알림은 null", example = "교체 D+3")
    val label: String?,

    @field:Schema(description = "발송 당시 다음 교체 예상일. 묶음·공지·기존 알림은 null")
    val nextReplacementDate: LocalDate?,

    @field:Schema(description = "대상 소모품 ID. 묶음·공지·기존 알림은 null")
    val itemId: Long?,

    @field:Schema(description = "알림 클릭 시 이동할 딥링크")
    val deepLink: String,
)
