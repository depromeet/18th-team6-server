package depromeet.hotsix.obrit.item.repository

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import depromeet.hotsix.obrit.item.entity.ItemOrder
import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.entity.QItem
import depromeet.hotsix.obrit.item.repository.predicate.ItemPredicateRepository
import java.time.LocalDate

class ItemQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
    private val itemPredicateRepository: ItemPredicateRepository,
) : ItemQueryRepository {

    override fun findItemList(
        userId: Long,
        order: ItemOrder,
        dDay: Int?,
        spareQuantity: Int?,
        cursor: Long?,
        today: LocalDate,
        size: Int,
    ): List<Item> {
        val item = QItem.item
        val cursorItem = cursor?.let { findCursorItem(userId, it) ?: return emptyList() }
        val predicates = listOfNotNull(
            item.userId.eq(userId),
            item.deletedAt.isNull,
            itemPredicateRepository.filterDday(item, today, dDay),
            itemPredicateRepository.filterSpareQuantity(item, spareQuantity),
            cursorItem?.let { cursorPredicate(item, order, it) },
        )

        return queryFactory
            .selectFrom(item)
            .where(*predicates.toTypedArray())
            .orderBy(*orderSpecifiers(item, order).toTypedArray())
            .limit(size.toLong())
            .fetch()
    }

    // cursor row가 soft-delete된 경우에도 sort 키를 그대로 살려 다음 페이지가 끊기지 않도록 deletedAt 조건을 제외한다.
    private fun findCursorItem(userId: Long, cursor: Long): Item? {
        val item = QItem.item

        return queryFactory
            .selectFrom(item)
            .where(
                item.userId.eq(userId),
                item.id.eq(cursor),
            )
            .fetchOne()
    }

    private fun cursorPredicate(item: QItem, order: ItemOrder, cursor: Item): BooleanExpression {
        val cursorId = requireNotNull(cursor.id)

        return when (order) {
            ItemOrder.REPLACEMENT_URGENT -> item.nextReplacementDate.gt(cursor.nextReplacementDate)
                .or(item.nextReplacementDate.eq(cursor.nextReplacementDate).and(item.id.gt(cursorId)))
            ItemOrder.SPARE_LOW -> item.quantity.gt(cursor.quantity)
                .or(item.quantity.eq(cursor.quantity).and(item.nextReplacementDate.gt(cursor.nextReplacementDate)))
                .or(
                    item.quantity.eq(cursor.quantity)
                        .and(item.nextReplacementDate.eq(cursor.nextReplacementDate))
                        .and(item.id.gt(cursorId)),
                )
            ItemOrder.USED_OLD -> item.lastReplacedDate.gt(cursor.lastReplacedDate)
                .or(item.lastReplacedDate.eq(cursor.lastReplacedDate).and(item.id.gt(cursorId)))
        }
    }

    private fun orderSpecifiers(item: QItem, order: ItemOrder): List<OrderSpecifier<*>> = when (order) {
        ItemOrder.REPLACEMENT_URGENT -> listOf(item.nextReplacementDate.asc(), item.id.asc())
        ItemOrder.SPARE_LOW -> listOf(item.quantity.asc(), item.nextReplacementDate.asc(), item.id.asc())
        ItemOrder.USED_OLD -> listOf(item.lastReplacedDate.asc(), item.id.asc())
    }
}
