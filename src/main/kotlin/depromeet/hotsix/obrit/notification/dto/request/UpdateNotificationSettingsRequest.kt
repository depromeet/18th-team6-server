package depromeet.hotsix.obrit.notification.dto.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime

@Schema(description = "알림 설정 수정 요청. 구버전 앱이 보내는 enabled, leadDays는 무시합니다.")
data class UpdateNotificationSettingsRequest(
    @field:Schema(description = "교체 예정 알림(교체일이 다가올 때와 당일) 수신 여부", example = "true")
    val preReplacementEnabled: Boolean,

    @field:Schema(description = "지연 알림(교체일이 지났을 때) 수신 여부", example = "true")
    val overdueEnabled: Boolean,

    @field:Schema(description = "여분 부족 알림 수신 여부", example = "true")
    val lowStockEnabled: Boolean,

    @field:Schema(
        description = "공통 알림 시간 (00:00~23:30, 30분 단위, 기본 09:00). 스케줄 시간대(기본 Asia/Seoul) 기준으로 실제 발송에 적용합니다.",
        example = "09:00",
        type = "string",
    )
    @field:JsonFormat(pattern = "HH:mm")
    val dispatchTime: LocalTime,
)
