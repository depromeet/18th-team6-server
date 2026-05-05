package depromeet.hotsix.obrit.item

import depromeet.hotsix.obrit.category.CategoryRepository
import depromeet.hotsix.obrit.common.BusinessException
import depromeet.hotsix.obrit.common.ResourceNotFoundException
import depromeet.hotsix.obrit.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class ItemService(
    private val itemRepository: ItemRepository,
    private val itemReplacementHistoryRepository: ItemReplacementHistoryRepository,
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun listItems(userId: Long): List<ItemResponse> =
        itemRepository.findActiveByUserId(userId).map { it.toResponse() }

    @Transactional
    fun createItem(userId: Long, request: CreateItemRequest): ItemResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found.") }
        val category = categoryRepository.findActiveById(request.categoryId)
            ?: throw ResourceNotFoundException("Category not found.")
        if (category.user != null && category.user?.id != userId) {
            throw ResourceNotFoundException("Category not found.")
        }

        val intervalDays = request.replacementIntervalDays ?: category.defaultReplacementIntervalDays
        val lastReplacedDate = request.lastReplacedDate ?: LocalDate.now()
        val item = Item(
            user = user,
            category = category,
            name = request.name.trim(),
            quantity = request.count,
            replacementIntervalDays = intervalDays,
            lastReplacedDate = lastReplacedDate,
            nextReplacementDate = lastReplacedDate.plusDays(intervalDays.toLong()),
        )

        return itemRepository.save(item).toResponse()
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

        return item.toResponse()
    }

    @Transactional
    fun deleteItem(userId: Long, itemId: Long) {
        findActiveItem(userId, itemId).softDelete()
    }

    @Transactional
    fun replaceItem(userId: Long, itemId: Long, request: CreateReplacementRequest): ItemResponse {
        val item = findActiveItem(userId, itemId)
        val replacedDate = request.replacedDate ?: LocalDate.now()

        item.replace(replacedDate)
        itemReplacementHistoryRepository.save(
            ItemReplacementHistory(
                item = item,
                replacedDate = replacedDate,
            ),
        )

        return item.toResponse()
    }

    private fun findActiveItem(userId: Long, itemId: Long): Item =
        itemRepository.findActiveByIdAndUserId(itemId, userId)
            ?: throw ResourceNotFoundException("Item not found.")
}
