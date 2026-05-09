package depromeet.hotsix.obrit.home.controller

import depromeet.hotsix.obrit.home.dto.MyStatusSummaryResponse
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

@Tag(name = "Home", description = "Home APIs")
@RestController
@RequestMapping("/home")
class HomeController(private val homeService: HomeService) {

    @Operation(
        summary = "홈 화면 - 전체 상태",
        description = "교체 관리 / 여분 관리 / 전체 상태를 표시 합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Home status returned.",
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
        @Parameter(description = "Development user id.", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
    ): OverallStatusResponse = homeService.getOverallStatus(userId)

    @Operation(
        summary = "홈 화면 - 내 상태 요약",
        description = "내 소모품 총 개수, 교체 위험 개수, 내 점수, 평균 점수를 반환합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "My status summary returned.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = MyStatusSummaryResponse::class),
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/my-summary")
    fun getMyStatusSummary(
        @Parameter(description = "Development user id.", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
    ): MyStatusSummaryResponse = homeService.getMyStatusSummary(userId)
}
