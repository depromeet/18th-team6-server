package depromeet.hotsix.obrit.notification.controller.docs

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.global.exception.ErrorResponse
import depromeet.hotsix.obrit.notification.dto.request.RegisterDeviceRequest
import depromeet.hotsix.obrit.notification.dto.response.RegisterDeviceResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "알림 기기", description = "푸시 알림 대상 기기 등록 API")
interface DeviceControllerApi {

    @Operation(
        summary = "알림 기기 등록",
        description = """
            앱 실행 시 발급받은 FID(Firebase Installation ID)를 등록합니다.
            이미 등록된 FID면 소유자를 현재 사용자로 변경합니다.
        """,
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "기기 등록 완료",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = RegisterDeviceResponse::class),
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
    fun registerDevice(
        @Parameter(description = "사용자 ID", required = true, example = "1", `in` = ParameterIn.HEADER)
        userId: Long,
        request: RegisterDeviceRequest,
    ): ApiResponse<RegisterDeviceResponse>

    @Operation(
        summary = "알림 기기 등록 해제",
        description = """
            로그아웃 시 호출합니다. 해당 기기로 더 이상 알림을 보내지 않습니다.
            등록되지 않은 FID이거나 다른 사용자의 기기여도 200을 반환합니다.
        """,
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "등록 해제 완료"),
        ],
    )
    fun unregisterDevice(
        @Parameter(description = "사용자 ID", required = true, example = "1", `in` = ParameterIn.HEADER)
        userId: Long,
        @Parameter(description = "Firebase Installation ID (FID)", required = true)
        fid: String,
    ): ApiResponse<Nothing?>
}
