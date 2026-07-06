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

    // findItemList와 동일한 필터(userId, deletedAt, dDay, spareQuantity) 조건의 전체 개수를 반환한다.
    // 커서/정렬은 페이지 경계에만 영향을 주므로 카운트에는 포함하지 않는다.
    fun countItemList(userId: Long, dDay: Int?, spareQuantity: Int?, today: LocalDate): Long
}
