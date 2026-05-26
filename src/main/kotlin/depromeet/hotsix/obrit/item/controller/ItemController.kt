package depromeet.hotsix.obrit.item.controller

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.item.controller.docs.ItemControllerApi
import depromeet.hotsix.obrit.item.dto.BulkCreateItemRequest
import depromeet.hotsix.obrit.item.dto.CreateItemRequest
import depromeet.hotsix.obrit.item.dto.CreateReplacementRequest
import depromeet.hotsix.obrit.item.dto.ItemDetailResponse
import depromeet.hotsix.obrit.item.dto.ItemResponse
import depromeet.hotsix.obrit.item.dto.ReplacementHistoryResponse
import depromeet.hotsix.obrit.item.dto.UpdateItemRequest
import depromeet.hotsix.obrit.item.dto.UpdateSpareCountRequest
import depromeet.hotsix.obrit.item.service.ItemService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
@RequestMapping("/items")
class ItemController(private val itemService: ItemService) : ItemControllerApi {

    @GetMapping
    override fun listItems(@RequestHeader("X-User-Id") userId: Long): ApiResponse<List<ItemResponse>> =
        ApiResponse.ok(itemService.listItems(userId))

    @GetMapping("/{itemId}")
    override fun getItemDetail(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable itemId: Long,
    ): ApiResponse<ItemDetailResponse> = ApiResponse.ok(itemService.getItemDetail(userId, itemId))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createItem(
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: CreateItemRequest,
    ): ApiResponse<ItemResponse> = ApiResponse.ok(itemService.createItem(userId, request))

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    override fun bulkCreateItems(
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: BulkCreateItemRequest,
    ): ApiResponse<List<ItemResponse>> = ApiResponse.ok(itemService.bulkCreateItems(userId, request))

    @PatchMapping("/{itemId}")
    override fun updateItem(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable itemId: Long,
        @Valid @RequestBody request: UpdateItemRequest,
    ): ApiResponse<ItemResponse> = ApiResponse.ok(itemService.updateItem(userId, itemId, request))

    @PatchMapping("/{itemId}/spare-count")
    override fun updateSpareCount(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable itemId: Long,
        @Valid @RequestBody request: UpdateSpareCountRequest,
    ): ApiResponse<ItemResponse> = ApiResponse.ok(itemService.updateSpareCount(userId, itemId, request))

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun deleteItem(@RequestHeader("X-User-Id") userId: Long, @PathVariable itemId: Long) {
        itemService.deleteItem(userId, itemId)
    }

    @PostMapping("/{itemId}/replacements")
    @ResponseStatus(HttpStatus.CREATED)
    override fun replaceItem(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable itemId: Long,
        @RequestBody request: CreateReplacementRequest,
    ): ApiResponse<ItemResponse> = ApiResponse.ok(itemService.replaceItem(userId, itemId, request))

    @GetMapping("/{itemId}/replacements")
    override fun listReplacements(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable itemId: Long,
        @RequestParam(defaultValue = "5")
        limit: Int,
    ): ApiResponse<List<ReplacementHistoryResponse>> =
        ApiResponse.ok(itemService.listReplacementHistories(userId, itemId, limit))
}
