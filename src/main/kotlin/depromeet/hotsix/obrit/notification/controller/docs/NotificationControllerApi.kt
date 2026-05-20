package depromeet.hotsix.obrit.notification.controller.docs

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.global.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "알림", description = "알림 API")
interface NotificationControllerApi {

    @Operation(
        summary = "알림 읽음 처리",
        description = "특정 알림을 읽음 상태로 변경합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "읽음 처리 완료"),
            SwaggerApiResponse(
                responseCode = "403",
                description = "알림을 읽을 권한이 없습니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "존재하지 않는 알림입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun markAsRead(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
        @Parameter(description = "알림 ID", required = true, example = "1")
        notificationId: Long,
    ): ApiResponse<Nothing?>

    @Operation(
        summary = "알림 전체 읽음 처리",
        description = "사용자의 모든 알림을 읽음 상태로 변경합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "전체 읽음 처리 완료"),
        ],
    )
    fun markAsReadAll(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
    ): ApiResponse<Nothing?>
}
