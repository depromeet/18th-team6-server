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

@Tag(name = "Home", description = "Home APIs")
@RestController
@RequestMapping("/home")
class HomeController(private val homeService: HomeService) {

    @Operation(
        summary = "Get home overall status",
        description = "Returns replacement, spare, and overall status for the home screen.",
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
}
