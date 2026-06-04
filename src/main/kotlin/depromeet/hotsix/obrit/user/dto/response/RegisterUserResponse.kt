package depromeet.hotsix.obrit.user.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원 등록 응답")
data class RegisterUserResponse(
    @field:Schema(description = "회원 ID (이후 API 호출 시 X-User-Id 헤더에 사용)", example = "1")
    val userId: Long,

    @field:Schema(description = "디바이스 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    val uuid: String,
)
