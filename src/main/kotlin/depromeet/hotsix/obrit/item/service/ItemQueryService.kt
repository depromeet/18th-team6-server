package depromeet.hotsix.obrit.item.service

import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.entity.ItemListSnapshot
import depromeet.hotsix.obrit.item.entity.ItemOrder
import depromeet.hotsix.obrit.item.entity.ItemSnapshot
import depromeet.hotsix.obrit.item.repository.ItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class ItemQueryService(private val itemRepository: ItemRepository) {

    fun findActiveSnapshotsByUserId(userId: Long): List<ItemSnapshot> =
        itemRepository.findActiveByUserId(userId).map { it.toSnapshot() }

    // 홈 화면 및 리스트 화면의 아이템 목록 페이지를 조회한다. Item 엔티티는 service 밖으로 노출하지 않고 ItemListSnapshot으로 변환해서 돌려준다.
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
