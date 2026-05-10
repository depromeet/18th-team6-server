package depromeet.hotsix.obrit.item.service

import depromeet.hotsix.obrit.global.readmodel.ItemListSnapshot
import depromeet.hotsix.obrit.global.readmodel.ItemOrder
import depromeet.hotsix.obrit.global.readmodel.ItemSnapshot
import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.repository.ItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class ItemQueryService(private val itemRepository: ItemRepository) {

    fun findActiveSnapshotsByUserId(userId: Long): List<ItemSnapshot> =
        itemRepository.findActiveByUserId(userId).map { it.toSnapshot() }

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
}
