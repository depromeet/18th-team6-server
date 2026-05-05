package depromeet.hotsix.obrit.item

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

@RestController
@RequestMapping("/items")
class ItemController(
    private val itemService: ItemService,
) {

    @GetMapping
    fun listItems(
        @RequestHeader("X-User-Id") userId: Long,
    ): List<ItemResponse> = itemService.listItems(userId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createItem(
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: CreateItemRequest,
    ): ItemResponse = itemService.createItem(userId, request)

    @PatchMapping("/{itemId}")
    fun updateItem(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable itemId: Long,
        @Valid @RequestBody request: UpdateItemRequest,
    ): ItemResponse = itemService.updateItem(userId, itemId, request)

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteItem(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable itemId: Long,
    ) {
        itemService.deleteItem(userId, itemId)
    }

    @PostMapping("/{itemId}/replacements")
    @ResponseStatus(HttpStatus.CREATED)
    fun replaceItem(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable itemId: Long,
        @RequestBody request: CreateReplacementRequest,
    ): ItemResponse = itemService.replaceItem(userId, itemId, request)
}
