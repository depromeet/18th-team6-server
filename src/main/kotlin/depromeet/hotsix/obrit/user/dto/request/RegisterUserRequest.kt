package depromeet.hotsix.obrit.user.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "회원 등록 요청")
data class RegisterUserRequest(
    @field:Schema(description = "인증 수단 (현재 uuid만 지원)", example = "uuid")
    @field:NotBlank(message = "인증 수단은 필수입니다.")
    val type: String,

    @field:Schema(description = "인증 값 (type=uuid일 때 디바이스 UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    @field:NotBlank(message = "인증 값은 필수입니다.")
    val value: String,
)
