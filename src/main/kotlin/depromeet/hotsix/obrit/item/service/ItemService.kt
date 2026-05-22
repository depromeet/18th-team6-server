package depromeet.hotsix.obrit.item.service

import depromeet.hotsix.obrit.category.service.CategoryQueryService
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.item.dto.BulkCreateItemRequest
import depromeet.hotsix.obrit.item.dto.CreateItemRequest
import depromeet.hotsix.obrit.item.dto.CreateReplacementRequest
import depromeet.hotsix.obrit.item.dto.ItemResponse
import depromeet.hotsix.obrit.item.dto.ReplacementHistoryResponse
import depromeet.hotsix.obrit.item.dto.UpdateItemRequest
import depromeet.hotsix.obrit.item.dto.UpdateSpareCountRequest
import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.entity.ItemReplacementHistory
import depromeet.hotsix.obrit.item.entity.ItemSnapshot
import depromeet.hotsix.obrit.item.repository.ItemReplacementHistoryRepository
import depromeet.hotsix.obrit.item.repository.ItemRepository
import depromeet.hotsix.obrit.user.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
class ItemService(
    private val itemRepository: ItemRepository,
    private val itemReplacementHistoryRepository: ItemReplacementHistoryRepository,
    private val categoryQueryService: CategoryQueryService,
    private val userService: UserService,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun listItems(userId: Long): List<ItemResponse> {
        val items = itemRepository.findActiveByUserId(userId)
        val categoryNamesById = categoryQueryService.findVisibleCategoryNames(userId, items.map { it.categoryId })

        return items.map { item ->
            item.toResponse(categoryName = categoryNameFor(item, categoryNamesById))
        }
    }

    @Transactional(readOnly = true)
    fun findActiveSnapshotsByUserId(userId: Long): List<ItemSnapshot> =
        itemRepository.findActiveByUserId(userId).map { it.toSnapshot() }

    @Transactional(readOnly = true)
    fun listReplacementHistories(userId: Long, itemId: Long, limit: Int): List<ReplacementHistoryResponse> {
        findActiveItem(userId, itemId)
        return itemReplacementHistoryRepository
            .findByItemIdOrderByReplacedDateDescIdDesc(itemId, PageRequest.ofSize(limit))
            .map {
                ReplacementHistoryResponse(
                    id = requireNotNull(it.id),
                    replacedDate = it.replacedDate,
                )
            }
    }

    @Transactional
    fun createItem(userId: Long, request: CreateItemRequest): ItemResponse {
        userService.validateUserExist(userId)
        return saveItem(userId, request)
    }

    @Transactional
    fun bulkCreateItems(userId: Long, request: BulkCreateItemRequest): List<ItemResponse> {
        userService.validateUserExist(userId)
        return request.items.map { saveItem(userId, it) }
    }

    private fun saveItem(userId: Long, request: CreateItemRequest): ItemResponse {
        val (categoryName, defaultReplacementIntervalDays) =
            categoryQueryService.getVisibleCategoryNameAndDefaultInterval(userId, request.categoryId)

        val intervalDays = request.replacementIntervalDays ?: defaultReplacementIntervalDays
        val lastReplacedDate = request.lastReplacedDate ?: LocalDate.now(clock)
        val item = Item(
            userId = userId,
            categoryId = request.categoryId,
            name = request.name.trim(),
            quantity = request.count,
            replacementIntervalDays = intervalDays,
            lastReplacedDate = lastReplacedDate,
            nextReplacementDate = lastReplacedDate.plusDays(intervalDays.toLong()),
        )

        return itemRepository.save(item).toResponse(categoryName = categoryName)
    }

    @Transactional
    fun updateItem(userId: Long, itemId: Long, request: UpdateItemRequest): ItemResponse {
        val item = findActiveItem(userId, itemId)
        val name = request.name?.also {
            if (it.isBlank()) {
                throw BusinessException("Item name cannot be blank.")
            }
        }

        item.update(
            name = name,
            quantity = request.count,
            replacementIntervalDays = request.replacementIntervalDays,
            lastReplacedDate = request.lastReplacedDate,
        )

        return item.toResponse(
            categoryName = categoryQueryService.getVisibleCategoryName(userId, item.categoryId),
        )
    }

    @Transactional
    fun updateSpareCount(userId: Long, itemId: Long, request: UpdateSpareCountRequest): ItemResponse {
        val count = request.count ?: throw BusinessException("여분 수량은 필수입니다.")
        if (count < 0) {
            throw BusinessException("여분 수량은 0 이상이어야 합니다.")
        }

        val item = findActiveItem(userId, itemId)
        item.updateSpareCount(count)

        return item.toResponse(
            categoryName = categoryQueryService.getVisibleCategoryName(userId, item.categoryId),
        )
    }

    @Transactional
    fun deleteItem(userId: Long, itemId: Long) {
        findActiveItem(userId, itemId).softDelete()
    }

    @Transactional
    fun replaceItem(userId: Long, itemId: Long, request: CreateReplacementRequest): ItemResponse {
        val item = findActiveItem(userId, itemId)
        val replacedDate = request.replacedDate ?: LocalDate.now(clock)

        item.replace(replacedDate)
        itemReplacementHistoryRepository.save(
            ItemReplacementHistory(
                item = item,
                replacedDate = replacedDate,
            ),
        )

        return item.toResponse(
            categoryName = categoryQueryService.getVisibleCategoryName(userId, item.categoryId),
        )
    }

    private fun findActiveItem(userId: Long, itemId: Long): Item =
        itemRepository.findActiveByIdAndUserId(itemId, userId)
            ?: throw ResourceNotFoundException("Item not found.")

    private fun categoryNameFor(item: Item, categoryNamesById: Map<Long, String>): String =
        categoryNamesById[item.categoryId] ?: throw ResourceNotFoundException("Category not found.")

    private fun Item.toResponse(categoryName: String): ItemResponse = ItemResponse(
        id = requireNotNull(id),
        categoryId = categoryId,
        categoryName = categoryName,
        name = name,
        count = quantity,
        replacementIntervalDays = replacementIntervalDays,
        lastReplacedDate = lastReplacedDate,
        nextReplacementDate = nextReplacementDate,
    )

    private fun Item.toSnapshot(): ItemSnapshot = ItemSnapshot(
        id = requireNotNull(id),
        name = name,
        nextReplacementDate = nextReplacementDate,
        quantity = quantity,
    )
}
