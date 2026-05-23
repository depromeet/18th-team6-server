package depromeet.hotsix.obrit.item.controller

import depromeet.hotsix.obrit.global.exception.ErrorResponse
import depromeet.hotsix.obrit.item.dto.CreateItemRequest
import depromeet.hotsix.obrit.item.dto.CreateReplacementRequest
import depromeet.hotsix.obrit.item.dto.ItemResponse
import depromeet.hotsix.obrit.item.dto.UpdateItemRequest
import depromeet.hotsix.obrit.item.service.ItemService
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
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

@Hidden
@Tag(name = "Items", description = "Item APIs")
@RestController
@RequestMapping("/items")
class ItemController(private val itemService: ItemService) {

    @Operation(
        summary = "List items",
        description = "Lists the user's items ordered by next replacement date.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Items returned.",
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
        @Parameter(description = "Development user id.", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
    ): List<ItemResponse> = itemService.listItems(userId)

    @Operation(
        summary = "Create item",
        description = "Creates an item in a preset or user-owned custom category.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Item created.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ItemResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid item request.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Category not found.",
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
        @Parameter(description = "Development user id.", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: CreateItemRequest,
    ): ItemResponse = itemService.createItem(userId, request)

    @Operation(
        summary = "Update item",
        description = "Updates mutable fields of an item.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Item updated.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ItemResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid item request.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Item not found.",
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
        @Parameter(description = "Development user id.", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable itemId: Long,
        @Valid @RequestBody request: UpdateItemRequest,
    ): ItemResponse = itemService.updateItem(userId, itemId, request)

    @Operation(
        summary = "Delete item",
        description = "Deletes an item.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Item deleted."),
            ApiResponse(
                responseCode = "404",
                description = "Item not found.",
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
        @Parameter(description = "Development user id.", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable itemId: Long,
    ) {
        itemService.deleteItem(userId, itemId)
    }

    @Operation(
        summary = "Record item replacement",
        description = "Records a replacement date and updates the item's next replacement date.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Replacement recorded.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ItemResponse::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Item not found.",
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
        @Parameter(description = "Development user id.", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable itemId: Long,
        @RequestBody request: CreateReplacementRequest,
    ): ItemResponse = itemService.replaceItem(userId, itemId, request)
}
