package depromeet.hotsix.obrit.user.controller.docs

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.global.exception.ErrorResponse
import depromeet.hotsix.obrit.user.dto.request.RegisterUserRequest
import depromeet.hotsix.obrit.user.dto.response.RegisterUserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "회원", description = "회원 API")
interface UserControllerApi {

    @Operation(
        summary = "회원 등록",
        description = "UUID 기반으로 회원을 등록합니다. 이미 등록된 UUID이면 기존 회원을 반환합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "회원 등록/조회 완료",
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "유효하지 않은 요청",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "지원하지 않는 인증 수단",
                                value = """{"message": "지원하지 않는 인증 수단입니다."}""",
                            ),
                            ExampleObject(
                                name = "UUID 형식 오류",
                                value = """{"message": "UUID 형식이 올바르지 않습니다."}""",
                            ),
                            ExampleObject(
                                name = "인증 값 누락",
                                value = """{"message": "인증 값은 필수입니다."}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun register(request: RegisterUserRequest): ApiResponse<RegisterUserResponse>
}
