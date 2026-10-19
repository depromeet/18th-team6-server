package depromeet.hotsix.obrit.notification.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import depromeet.hotsix.obrit.notification.entity.NotificationPermissionStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime

@Schema(description = "알림 설정 응답")
data class NotificationSettingsResponse(
    @field:Schema(description = "교체 예정 알림(교체일이 다가올 때와 당일) 수신 여부", example = "true")
    val preReplacementEnabled: Boolean,

    @field:Schema(description = "지연 알림(교체일이 지났을 때) 수신 여부", example = "true")
    val overdueEnabled: Boolean,

    @field:Schema(description = "여분 부족 알림 수신 여부", example = "true")
    val lowStockEnabled: Boolean,

    @field:Schema(
        description = "실제 발송에 적용하는 공통 알림 시간 (30분 단위, 기본 09:00). 기본 시간대는 Asia/Seoul입니다.",
        example = "09:00",
        type = "string",
    )
    @field:JsonFormat(pattern = "HH:mm")
    val dispatchTime: LocalTime,

    @field:Schema(description = "기기 알림 권한 상태", example = "NOT_REQUESTED")
    val permissionStatus: NotificationPermissionStatus,

    @field:Schema(description = "구버전 앱 호환용 고정값(true). 다음 배포에서 삭제합니다.", example = "true", deprecated = true)
    val enabled: Boolean = true,

    @field:Schema(description = "구버전 앱 호환용 고정값(3). 다음 배포에서 삭제합니다.", example = "3", deprecated = true)
    val leadDays: Int = 3,
)
