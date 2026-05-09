package depromeet.hotsix.obrit.item.service

import depromeet.hotsix.obrit.global.readmodel.ItemSnapshot
import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.repository.ItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ItemQueryService(private val itemRepository: ItemRepository) {

    fun findActiveSnapshotsByUserId(userId: Long): List<ItemSnapshot> =
        itemRepository.findActiveByUserId(userId).map { it.toSnapshot() }

    private fun Item.toSnapshot(): ItemSnapshot = ItemSnapshot(
        id = requireNotNull(id),
        name = name,
        nextReplacementDate = nextReplacementDate,
        quantity = quantity,
    )
}
