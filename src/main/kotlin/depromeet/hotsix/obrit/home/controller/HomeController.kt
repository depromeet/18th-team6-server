package depromeet.hotsix.obrit.home.controller

import depromeet.hotsix.obrit.home.dto.OverallStatusResponse
import depromeet.hotsix.obrit.home.service.HomeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Home", description = "홈 화면 관련 API")
@RestController
@RequestMapping("/home")
class HomeController(private val homeService: HomeService) {

    @Operation(
        summary = "홈 화면 - 전체 상태 조회",
        description = """
            홈 화면 상단에 표시할 사용자의 전체 상태를 반환합니다.

            응답 필드:
            - replacement: 교체 관리 상태 (GOOD / WARNING / DANGER)
              · 교체 시기가 지난 아이템 비율과 평균 교체 점수를 기반으로 산정됩니다.
            - spare: 여분 관리 상태 (GOOD / WARNING / DANGER)
              · 여분 수량이 부족한 아이템 비율을 기반으로 산정됩니다.
            - overall: 전체 종합 상태 (PERFECT / GOOD / WARNING / DANGER)
              · replacement, spare 상태의 조합으로 결정됩니다.

        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "홈 전체 상태 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = OverallStatusResponse::class),
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/overall-status")
    fun getOverallStatus(
        @Parameter(
            description = "사용자 ID (인증 도입 전 임시 헤더, 추후 인증 토큰으로 대체 예정)",
            required = true,
            example = "1",
        )
        @RequestHeader("X-User-Id") userId: Long,
    ): OverallStatusResponse = homeService.getOverallStatus(userId)
}
