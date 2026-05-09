package depromeet.hotsix.obrit.home.controller

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.home.dto.HomeBucketsResponse
import depromeet.hotsix.obrit.home.dto.MyStatusSummaryResponse
import depromeet.hotsix.obrit.home.dto.OverallStatusResponse
import depromeet.hotsix.obrit.home.service.HomeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

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
            SwaggerApiResponse(
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
    ): ApiResponse<OverallStatusResponse> = ApiResponse.ok(homeService.getOverallStatus(userId))

    @Operation(
        summary = "홈 화면 - 내 상태 요약",
        description = "내 소모품 총 개수, 교체 위험 개수, 내 점수, 평균 점수를 반환합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
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
    ): ApiResponse<MyStatusSummaryResponse> = ApiResponse.ok(homeService.getMyStatusSummary(userId))

    @Operation(
        summary = "홈 화면 - 버킷별 개수/목록",
        description = "여분 보유 여부와 교체 시점으로 나눈 여섯 개 버킷별 item 개수와 목록을 반환합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "Home buckets returned.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = HomeBucketsResponse::class),
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/buckets")
    fun getBuckets(
        @Parameter(description = "Development user id.", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
    ): ApiResponse<HomeBucketsResponse> = ApiResponse.ok(homeService.getBuckets(userId))
}
