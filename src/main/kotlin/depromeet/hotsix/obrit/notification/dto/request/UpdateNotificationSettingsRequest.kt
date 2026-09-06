package depromeet.hotsix.obrit.notification.dto.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime

@Schema(description = "알림 설정 수정 요청")
data class UpdateNotificationSettingsRequest(
    @field:Schema(description = "알림 수신 여부. 끄면 유형별 설정과 무관하게 알림이 발송되지 않습니다.", example = "true")
    val enabled: Boolean,

    @field:Schema(description = "사전 알림(교체일이 다가올 때) 수신 여부", example = "true")
    val preReplacementEnabled: Boolean,

    @field:Schema(description = "지연 알림(교체일이 지났을 때) 수신 여부", example = "true")
    val overdueEnabled: Boolean,

    @field:Schema(description = "여분 부족 알림 수신 여부", example = "true")
    val lowStockEnabled: Boolean,

    @field:Schema(description = "사전 알림 선행 일수 (1~7일)", example = "3")
    val leadDays: Int,

    @field:Schema(description = "알림 발송 시각. 30분 단위이며 08:00~21:30만 설정할 수 있습니다.", example = "09:00", type = "string")
    @field:JsonFormat(pattern = "HH:mm")
    val dispatchTime: LocalTime,
)
