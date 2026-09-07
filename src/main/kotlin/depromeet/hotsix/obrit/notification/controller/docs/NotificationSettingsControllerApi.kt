package depromeet.hotsix.obrit.notification.controller.docs

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.global.exception.ErrorResponse
import depromeet.hotsix.obrit.notification.dto.response.NotificationSettingsResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "알림 설정", description = "유저별 알림 설정 API")
interface NotificationSettingsControllerApi {

    @Operation(
        summary = "알림 설정 조회",
        description = "사용자의 알림 설정을 조회합니다. 저장한 설정이 없으면 기본값을 반환합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "조회 완료",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = NotificationSettingsResponse::class),
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "존재하지 않는 사용자입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun getSettings(
        @Parameter(description = "사용자 ID", required = true, example = "1", `in` = ParameterIn.HEADER)
        userId: Long,
    ): ApiResponse<NotificationSettingsResponse>
}
