package depromeet.hotsix.obrit.notification.controller.docs

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.global.exception.ErrorResponse
import depromeet.hotsix.obrit.notification.dto.request.RegisterFcmTokenRequest
import depromeet.hotsix.obrit.notification.dto.response.RegisterFcmTokenResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "FCM 토큰", description = "FCM 토큰 관리 API")
interface FcmTokenControllerApi {

    @Operation(
        summary = "FCM 토큰 등록",
        description = "디바이스의 FCM 토큰을 등록합니다. 이미 등록된 토큰이면 소유자를 변경합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "토큰 등록 완료",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ApiResponse::class),
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "유효하지 않은 요청입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun registerToken(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
        request: RegisterFcmTokenRequest,
    ): ApiResponse<RegisterFcmTokenResponse>
}
