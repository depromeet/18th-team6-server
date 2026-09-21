package depromeet.hotsix.obrit.notification.controller.docs

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.global.exception.ErrorResponse
import depromeet.hotsix.obrit.notification.dto.request.ReportNotificationPermissionRequest
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
interface NotificationPermissionControllerApi {

    @Operation(
        summary = "알림 권한 상태 보고",
        description = "기기 알림 권한 상태를 서버에 기록합니다. 온보딩 권한 요청 응답 직후와 앱 실행 시 호출합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "기록 완료",
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
    fun reportPermission(
        @Parameter(description = "사용자 ID", required = true, example = "1", `in` = ParameterIn.HEADER)
        userId: Long,
        request: ReportNotificationPermissionRequest,
    ): ApiResponse<NotificationSettingsResponse>
}
