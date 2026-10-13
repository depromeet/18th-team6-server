package depromeet.hotsix.obrit.notification.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "알림 기기 등록 응답")
data class RegisterDeviceResponse(
    @field:Schema(description = "기기 등록 ID")
    val id: Long,

    @field:Schema(description = "사용자 ID")
    val userId: Long,

    @field:Schema(description = "Firebase Installation ID (FID)")
    val fid: String,

    @field:Schema(description = "등록 일시")
    val createdAt: LocalDateTime,
)
