package depromeet.hotsix.obrit.notification.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "FCM 토큰 등록 응답")
data class RegisterFcmTokenResponse(
    @field:Schema(description = "토큰 ID")
    val id: Long,

    @field:Schema(description = "사용자 ID")
    val userId: Long,

    @field:Schema(description = "FCM 토큰")
    val token: String,

    @field:Schema(description = "생성 일시")
    val createdAt: LocalDateTime,
)
