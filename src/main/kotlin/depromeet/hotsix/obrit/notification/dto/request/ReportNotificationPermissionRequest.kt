package depromeet.hotsix.obrit.notification.dto.request

import depromeet.hotsix.obrit.notification.entity.NotificationPermissionStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "알림 권한 상태 보고 요청")
data class ReportNotificationPermissionRequest(
    @field:Schema(
        description = "기기 알림 권한 상태. 온보딩 권한 요청 응답 직후와 앱 실행 시 보고합니다.",
        example = "GRANTED",
    )
    val permissionStatus: NotificationPermissionStatus,
)
