package depromeet.hotsix.obrit.notification.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "알림 기기 등록 요청")
data class RegisterDeviceRequest(
    @field:Schema(description = "Firebase Installation ID (FID)", example = "cX8fRzQpS0aBvK2mL9wNdT")
    @field:NotBlank(message = "FID는 필수입니다.")
    val fid: String,
)
