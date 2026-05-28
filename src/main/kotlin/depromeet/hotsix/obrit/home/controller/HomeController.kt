package depromeet.hotsix.obrit.home.controller

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.global.paging.CursorSliceResponse
import depromeet.hotsix.obrit.home.dto.HomeBucketsResponse
import depromeet.hotsix.obrit.home.dto.HomeItemCard
import depromeet.hotsix.obrit.home.dto.MyStatusSummaryResponse
import depromeet.hotsix.obrit.home.dto.OverallStatusResponse
import depromeet.hotsix.obrit.home.service.HomeService
import depromeet.hotsix.obrit.item.entity.ItemOrder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

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
            SwaggerApiResponse(
                responseCode = "200",
                description = "홈 전체 상태 조회 성공",
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
        description = """
            위험도 기준 두 개 버킷(위험/경고)별 item 개수와 목록을 반환합니다.

            분류 기준:
            - DANGER(위험): 여분 없음 + 교체 지남/임박, 또는 여분 있음 + 교체 지남
            - WARNING(경고): 여분 있음 + 교체 임박, 또는 여분 없음 + 교체 여유

            동작:
            - 응답의 buckets는 항상 [DANGER, WARNING] 순서로 두 버킷 모두 반환됩니다.
              해당 버킷에 속한 아이템이 없으면 count=0, items=[] 로 반환됩니다.
            - 양호(GOOD) 상태의 아이템은 어느 버킷에도 포함되지 않습니다.
            - 각 버킷의 items는 교체 D-day 순으로 정렬됩니다.
              교체일이 가장 많이 지난 아이템(D+N이 큰 순)이 먼저 노출됩니다.
            - 각 아이템의 itemBucket 필드(6종)는 여분 유무 × 교체 시기 조합 세부 상태로,
              클라이언트 카드 배경 등 UI 분기에 사용합니다.
        """,
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "Home buckets returned.",
            ),
        ],
    )
    @GetMapping("/buckets")
    fun getBuckets(
        @Parameter(description = "Development user id.", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
    ): ApiResponse<HomeBucketsResponse> = ApiResponse.ok(homeService.getBuckets(userId))

    @Operation(
        summary = "홈 화면 - 아이템 무한 스크롤 목록",
        description = """
            홈/리스트 화면의 아이템 카드 목록을 커서 기반 무한 스크롤로 반환합니다.

            동작:
            - 정렬(order), D-day, 최소 여분 수량 필터를 조합해 결과를 좁힙니다.
            - 응답의 nextCursor를 다음 요청의 cursor 파라미터로 그대로 넘기면 다음 페이지를 조회할 수 있습니다.
            - hasNext가 false이면 더 이상 페이지가 없습니다.
            - 각 아이템 카드의 itemBucket 필드(6종)는 여분 유무 × 교체 시기 조합 세부 상태로,
              클라이언트 카드 배경 등 UI 분기에 사용합니다.
        """,
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "홈 아이템 목록 조회 성공",
            ),
        ],
    )
    @GetMapping("/items")
    fun getItems(
        @Parameter(
            description = "사용자 ID (인증 도입 전 임시 헤더, 추후 인증 토큰으로 대체 예정)",
            required = true,
            example = "1",
        )
        @RequestHeader("X-User-Id") userId: Long,
        @Parameter(
            description = "정렬 기준 (기본값: USED_OLD)\n" +
                "- REPLACEMENT_URGENT: 교체가 임박한 순\n" +
                "- SPARE_LOW: 여분 수량이 적은 순\n" +
                "- USED_OLD: 오래 사용 중인 순\n" +
                "- ITEM_NAME: 아이템 이름 가나다 순",
            example = "USED_OLD",
        )
        @RequestParam(required = false, defaultValue = "USED_OLD") order: ItemOrder,
        @Parameter(
            description = "D-day 필터. 오늘부터 N일 이내(당일 포함)에 교체가 필요한 아이템만 반환합니다. " +
                "예) 30이면 오늘부터 30일 이내에 교체 예정인 아이템만 조회됩니다. " +
                "값이 없으면 D-day 필터를 적용하지 않습니다.",
            example = "30",
        )
        @RequestParam(required = false) dDay: Int?,
        @Parameter(
            description = "최소 여분 수량 필터. 여분 수량이 이 값 이상인 아이템만 반환합니다. " +
                "값이 없으면 여분 수량 필터를 적용하지 않습니다.",
            example = "2",
        )
        @RequestParam(required = false) spareQuantity: Int?,
        @Parameter(
            description = "커서. 직전 응답의 nextCursor 값을 그대로 넘깁니다. " +
                "첫 페이지 요청에는 비워서 보내면 됩니다.",
            example = "1001",
        )
        @RequestParam(required = false) cursor: Long?,
        @Parameter(
            description = "한 페이지 크기. 1~20 범위로 보정되며 기본값은 20입니다.",
            example = "20",
        )
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ApiResponse<CursorSliceResponse<HomeItemCard>> = ApiResponse.ok(
        homeService.getItems(
            userId = userId,
            order = order,
            dDay = dDay,
            spareQuantity = spareQuantity,
            cursor = cursor,
            size = size,
        ),
    )
}
