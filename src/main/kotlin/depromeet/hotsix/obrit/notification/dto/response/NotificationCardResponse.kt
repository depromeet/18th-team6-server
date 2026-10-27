package depromeet.hotsix.obrit.notification.dto.response

import depromeet.hotsix.obrit.notification.entity.NotificationType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "알림함 개별 카드. 모든 값은 발송 당시 스냅샷이며 이후 소모품 수정·삭제를 반영하지 않는다")
data class NotificationCardResponse(
    @field:Schema(description = "알림 카드 ID. 읽음 처리에는 상위 알림 ID를 사용")
    val entryId: Long,

    @field:Schema(description = "카드 대표 유형. 일정 사유(PRE_REPLACEMENT·OVERDUE)가 있으면 그 사유, 여분 부족만 있으면 LOW_STOCK, 공지는 NOTICE")
    val type: NotificationType,

    @field:Schema(description = "카드 제목", example = "교체 시기가 지났어요")
    val title: String,

    @field:Schema(description = "카드 내용. 여분 부족이 겹치면 문구가 덧붙음", example = "수건 교체 예정일이 지났어요 여분도 없어요")
    val content: String,

    @field:Schema(description = "발송 당시 소모품 이름. 공지는 null")
    val itemName: String?,

    @field:Schema(description = "발송 당시 라벨. 여분 부족이 겹치면 ' · 여분 부족'이 붙음. 공지는 null", example = "교체 D+1 · 여분 부족")
    val label: String?,

    @field:Schema(description = "발송 당시 여분 부족 여부. 공지는 false")
    val hasLowStock: Boolean,

    @field:Schema(description = "발송 당시 다음 교체 예상일. 공지는 null")
    val nextReplacementDate: LocalDate?,

    @field:Schema(description = "대상 소모품 ID. 발송 후 삭제된 소모품일 수 있음. 공지는 null")
    val itemId: Long?,

    @field:Schema(description = "카드 클릭 시 이동할 딥링크. 삭제된 소모품이면 상세 조회가 404를 반환", example = "obrit://items/10")
    val deepLink: String,
)
