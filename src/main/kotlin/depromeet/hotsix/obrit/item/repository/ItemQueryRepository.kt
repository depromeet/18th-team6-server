package depromeet.hotsix.obrit.item.repository

import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.entity.ItemOrder
import java.time.LocalDate

interface ItemQueryRepository {

    fun findItemList(
        userId: Long,
        order: ItemOrder,
        dDay: Int?,
        spareQuantity: Int?,
        cursor: Long?,
        today: LocalDate,
        size: Int,
    ): List<Item>
}
