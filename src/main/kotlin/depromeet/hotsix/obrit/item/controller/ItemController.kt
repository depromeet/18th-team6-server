package depromeet.hotsix.obrit.item.controller

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.global.exception.ErrorResponse
import depromeet.hotsix.obrit.item.dto.CreateItemRequest
import depromeet.hotsix.obrit.item.dto.CreateReplacementRequest
import depromeet.hotsix.obrit.item.dto.ItemResponse
import depromeet.hotsix.obrit.item.dto.UpdateItemRequest
import depromeet.hotsix.obrit.item.service.ItemService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Items", description = "아이템 API")
@RestController
@RequestMapping("/items")
class ItemController(private val itemService: ItemService) {

    @Operation(
        summary = "아이템 목록 조회",
        description = "다음 교체일 기준으로 정렬된 사용자의 아이템 목록을 조회합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "아이템 목록 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        array = ArraySchema(schema = Schema(implementation = ItemResponse::class)),
                    ),
                ],
            ),
        ],
    )
    @GetMapping
    fun listItems(
        @Parameter(description = "개발용 사용자 ID", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
    ): ApiResponse<List<ItemResponse>> = ApiResponse.ok(itemService.listItems(userId))

    @Operation(
        summary = "아이템 생성",
        description = "프리셋 카테고리 또는 사용자가 보유한 커스텀 카테고리에 아이템을 생성합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "201",
                description = "아이템 생성 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ItemResponse::class),
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "유효하지 않은 아이템 요청",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "카테고리를 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createItem(
        @Parameter(description = "개발용 사용자 ID", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: CreateItemRequest,
    ): ApiResponse<ItemResponse> = ApiResponse.ok(itemService.createItem(userId, request))

    @Operation(
        summary = "아이템 수정",
        description = "아이템의 변경 가능한 필드를 수정합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "아이템 수정 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ItemResponse::class),
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "유효하지 않은 아이템 요청",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "아이템을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    @PatchMapping("/{itemId}")
    fun updateItem(
        @Parameter(description = "개발용 사용자 ID", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable itemId: Long,
        @Valid @RequestBody request: UpdateItemRequest,
    ): ApiResponse<ItemResponse> = ApiResponse.ok(itemService.updateItem(userId, itemId, request))

    @Operation(
        summary = "아이템 삭제",
        description = "아이템을 삭제합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "204", description = "아이템 삭제 성공"),
            SwaggerApiResponse(
                responseCode = "404",
                description = "아이템을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteItem(
        @Parameter(description = "개발용 사용자 ID", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable itemId: Long,
    ) {
        itemService.deleteItem(userId, itemId)
    }

    @Operation(
        summary = "아이템 교체 기록",
        description = "교체일을 기록하고 아이템의 다음 교체일을 갱신합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "201",
                description = "교체 기록 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ItemResponse::class),
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "아이템을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/{itemId}/replacements")
    @ResponseStatus(HttpStatus.CREATED)
    fun replaceItem(
        @Parameter(description = "개발용 사용자 ID", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable itemId: Long,
        @RequestBody request: CreateReplacementRequest,
    ): ApiResponse<ItemResponse> = ApiResponse.ok(itemService.replaceItem(userId, itemId, request))
}
