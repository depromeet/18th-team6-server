package depromeet.hotsix.obrit.notification.dto.request

import jakarta.validation.constraints.NotBlank

data class RegisterFcmTokenRequest(
    @field:NotBlank(message = "FCM 토큰은 필수입니다.")
    val token: String,
)
