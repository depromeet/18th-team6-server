package depromeet.hotsix.obrit.item.repository

import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.entity.ItemOrder
import java.time.LocalDate

interface ItemQueryRepository {

    // 정렬/필터/커서 기반으로 아이템 목록 한 페이지를 조회한다.
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
