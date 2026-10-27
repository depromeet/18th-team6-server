package depromeet.hotsix.obrit.notification.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "알림 목록 항목")
data class ListNotificationResponse(
    @field:Schema(description = "알림 ID")
    val id: Long,

    @field:Schema(description = "알림 제목. 푸시로 발송된 문구이며 묶음이면 요약 문구")
    val title: String,

    @field:Schema(description = "알림 내용. 푸시로 발송된 문구", example = "수건 외 1건, 확인해보세요")
    val content: String,

    // Swagger가 is 접두사를 떼고 표시하지 않도록 getter 기준으로 이름과 설명을 지정한다.
    @get:JsonProperty("isRead")
    @get:Schema(description = "읽음 여부. 묶음 알림도 알림 단위로 하나")
    val isRead: Boolean,

    @field:Schema(description = "수신 일시")
    val createdAt: LocalDateTime,

    @field:Schema(description = "발송 당시 라벨. 묶음·공지·기존 알림은 null", example = "교체 D+3")
    val label: String?,

    @field:Schema(description = "발송 당시 다음 교체 예상일. 묶음·공지·기존 알림은 null")
    val nextReplacementDate: LocalDate?,

    @field:Schema(description = "대상 소모품 ID. 묶음·공지·기존 알림은 null")
    val itemId: Long?,

    @field:Schema(description = "알림 클릭 시 이동할 딥링크. 단건은 소모품 상세, 묶음·공지·기존 알림은 홈", example = "obrit://home")
    val deepLink: String,

    // Swagger가 is 접두사를 떼고 표시하지 않도록 getter 기준으로 이름과 설명을 지정한다.
    @get:JsonProperty("isBundled")
    @get:Schema(description = "묶음 알림 여부. 카드가 2개 이상이면 true")
    val isBundled: Boolean,

    @field:Schema(description = "소모품별 카드 목록. 급한 순 정렬. 카드 저장 이전의 기존 알림은 빈 배열")
    val cards: List<NotificationCardResponse>,
)
