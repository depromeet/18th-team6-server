package depromeet.hotsix.obrit.item.service

import depromeet.hotsix.obrit.category.service.CategoryQueryService
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.item.dto.BulkCreateItemRequest
import depromeet.hotsix.obrit.item.dto.CreateItemRequest
import depromeet.hotsix.obrit.item.dto.CreateReplacementRequest
import depromeet.hotsix.obrit.item.dto.ItemCategoryResponse
import depromeet.hotsix.obrit.item.dto.ItemDetailResponse
import depromeet.hotsix.obrit.item.dto.ItemReplacementResponse
import depromeet.hotsix.obrit.item.dto.ItemResponse
import depromeet.hotsix.obrit.item.dto.ReplacementHistoryResponse
import depromeet.hotsix.obrit.item.dto.UpdateItemRequest
import depromeet.hotsix.obrit.item.dto.UpdateSpareCountRequest
import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.entity.ItemDetailStatus
import depromeet.hotsix.obrit.item.entity.ItemListSnapshot
import depromeet.hotsix.obrit.item.entity.ItemOrder
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
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.round

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
    fun getItemDetail(userId: Long, itemId: Long): ItemDetailResponse {
        val item = findActiveItem(userId, itemId)
        val category = categoryQueryService.getVisibleCategorySnapshot(userId, item.categoryId)
        val today = LocalDate.now(clock)
        val dday = ChronoUnit.DAYS.between(today, item.nextReplacementDate).toInt()
        val usedDays = ChronoUnit.DAYS.between(item.lastReplacedDate, today).toInt().coerceAtLeast(0)
        val recentReplacements = recentReplacementResponses(item, usedDays)
        val progressPercentage = (usedDays.toDouble() / category.defaultReplacementIntervalDays * 100)
            .roundToOneDecimal()

        return ItemDetailResponse(
            id = requireNotNull(item.id),
            name = item.name,
            category = ItemCategoryResponse(id = category.id, name = category.name),
            iconUrl = category.iconUrl,
            status = detailStatus(dday, item.quantity),
            dday = dday,
            ddayLabel = ddayLabel(dday),
            spareQuantity = item.quantity,
            lastReplacedDate = item.lastReplacedDate,
            nextReplacementDate = item.nextReplacementDate,
            usedDays = usedDays,
            myAverageCycleDays = averageCycleDays(recentReplacements),
            recommendedCycleDays = category.defaultReplacementIntervalDays,
            progressPercentage = progressPercentage,
            recentReplacements = recentReplacements,
        )
    }

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

    fun findItemListSnapshots(
        userId: Long,
        order: ItemOrder,
        dDay: Int?,
        spareQuantity: Int?,
        cursor: Long?,
        today: LocalDate,
        size: Int,
    ): List<ItemListSnapshot> = itemRepository.findItemList(
        userId = userId,
        order = order,
        dDay = dDay,
        spareQuantity = spareQuantity,
        cursor = cursor,
        today = today,
        size = size,
    ).map { it.toItemListSnapshot() }

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
        val today = LocalDate.now(clock)
        val lastReplacedDate = request.lastReplacementPeriod?.toDate(today) ?: today
        val item = Item(
            userId = userId,
            categoryId = request.categoryId,
            name = request.name.trim(),
            quantity = request.spareQuantity,
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
            quantity = request.spareQuantity,
            replacementIntervalDays = request.replacementIntervalDays,
            lastReplacedDate = request.lastReplacedDate,
        )

        return item.toResponse(
            categoryName = categoryQueryService.getVisibleCategoryName(userId, item.categoryId),
        )
    }

    @Transactional
    fun updateSpareCount(userId: Long, itemId: Long, request: UpdateSpareCountRequest): ItemResponse {
        val spareQuantity = request.spareQuantity ?: throw BusinessException("여분 수량은 필수입니다.")
        if (spareQuantity < 0) {
            throw BusinessException("여분 수량은 0 이상이어야 합니다.")
        }

        val item = findActiveItem(userId, itemId)
        item.updateSpareCount(spareQuantity)

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

    private fun recentReplacementResponses(item: Item, usedDays: Int): List<ItemReplacementResponse> {
        val itemId = requireNotNull(item.id)
        val replacementEventOrder = compareBy<ReplacementEvent> { it.date }.thenBy { it.id }
        val histories = itemReplacementHistoryRepository
            .findByItemIdOrderByReplacedDateDescIdDesc(itemId, PageRequest.ofSize(5))
        val historyEvents = histories.map { history ->
            ReplacementEvent(
                id = requireNotNull(history.id),
                date = history.replacedDate,
                isCurrent = false,
            )
        }
        val sortedHistoryEvents = historyEvents.sortedWith(replacementEventOrder)
        val currentHistoryIndex = sortedHistoryEvents.indexOfLast { it.date == item.lastReplacedDate }
        val events = if (currentHistoryIndex >= 0) {
            sortedHistoryEvents.mapIndexed { index, event ->
                if (index == currentHistoryIndex) event.copy(isCurrent = true) else event
            }
        } else {
            sortedHistoryEvents + ReplacementEvent(
                id = itemId,
                date = item.lastReplacedDate,
                isCurrent = true,
            )
        }
        val sortedEvents = events.sortedWith(replacementEventOrder)
        val eventsWithCycleDays = sortedEvents.mapIndexed { index, event ->
            val cycleDays = when {
                event.isCurrent -> usedDays
                index == 0 -> item.replacementIntervalDays
                else -> ChronoUnit.DAYS.between(sortedEvents[index - 1].date, event.date).toInt()
            }
            event to cycleDays
        }

        return eventsWithCycleDays.takeLast(5).map { (event, cycleDays) ->
            ItemReplacementResponse(
                id = event.id,
                date = event.date,
                cycleDays = cycleDays,
                isCurrent = event.isCurrent,
            )
        }
    }

    private fun detailStatus(dday: Int, spareCount: Int): ItemDetailStatus = when {
        dday <= 0 -> ItemDetailStatus.DANGER
        dday <= REPLACEMENT_WARNING_DAYS -> ItemDetailStatus.WARNING
        spareCount == 0 -> ItemDetailStatus.LOW_STOCK
        else -> ItemDetailStatus.GOOD
    }

    private fun ddayLabel(dday: Int): String = when {
        dday == 0 -> "D-day"
        dday > 0 -> "D-$dday"
        else -> "D+${abs(dday)}"
    }

    private fun averageCycleDays(recentReplacements: List<ItemReplacementResponse>): Double {
        if (recentReplacements.isEmpty()) {
            return 0.0
        }

        return recentReplacements.map { it.cycleDays }.average().roundToOneDecimal()
    }

    private fun Double.roundToOneDecimal(): Double = round(this * 10) / 10

    private fun Item.toResponse(categoryName: String): ItemResponse = ItemResponse(
        id = requireNotNull(id),
        categoryId = categoryId,
        categoryName = categoryName,
        name = name,
        spareQuantity = quantity,
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

    private fun Item.toItemListSnapshot(): ItemListSnapshot = ItemListSnapshot(
        id = requireNotNull(id),
        name = name,
        quantity = quantity,
        lastReplacedDate = lastReplacedDate,
        nextReplacementDate = nextReplacementDate,
    )

    private data class ReplacementEvent(val id: Long, val date: LocalDate, val isCurrent: Boolean)

    companion object {
        private const val REPLACEMENT_WARNING_DAYS = 3
    }
}
