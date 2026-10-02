package depromeet.hotsix.obrit.notification.dto.response

import depromeet.hotsix.obrit.notification.entity.NotificationType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "알림함 개별 카드")
data class NotificationCardResponse(
    @field:Schema(description = "알림 카드 ID")
    val entryId: Long,

    @field:Schema(description = "알림 유형")
    val type: NotificationType,

    @field:Schema(description = "카드 제목")
    val title: String,

    @field:Schema(description = "카드 내용")
    val content: String,

    @field:Schema(description = "발송 당시 소모품 이름. 공지는 null")
    val itemName: String?,

    @field:Schema(description = "발송 당시 라벨. 공지는 null", example = "교체 D+3")
    val label: String?,

    @field:Schema(description = "발송 당시 다음 교체 예상일. 공지는 null")
    val nextReplacementDate: LocalDate?,

    @field:Schema(description = "대상 소모품 ID. 공지는 null")
    val itemId: Long?,

    @field:Schema(description = "카드 클릭 시 이동할 딥링크")
    val deepLink: String,
)
