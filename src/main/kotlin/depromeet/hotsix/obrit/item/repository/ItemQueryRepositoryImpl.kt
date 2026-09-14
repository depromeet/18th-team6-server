package depromeet.hotsix.obrit.item.repository

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.entity.ItemOrder
import depromeet.hotsix.obrit.item.entity.QItem
import depromeet.hotsix.obrit.item.repository.predicate.ItemPredicateRepository
import java.time.LocalDate

class ItemQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
    private val itemPredicateRepository: ItemPredicateRepository,
) : ItemQueryRepository {

    // 아이템(userId 일치 + deletedAt null)을 d-day 및 여분 수량 필터와 커서로 잘라 size 만큼 가져온다.
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
        val predicates = basePredicates(item, userId, today, dDay, spareQuantity) +
            listOfNotNull(cursorItem?.let { cursorPredicate(item, order, it) })

        return queryFactory
            .selectFrom(item)
            .where(*predicates.toTypedArray())
            .orderBy(*orderSpecifiers(item, order).toTypedArray())
            .limit(size.toLong())
            .fetch()
    }

    override fun countItemList(userId: Long, dDay: Int?, spareQuantity: Int?, today: LocalDate): Long {
        val item = QItem.item
        val predicates = basePredicates(item, userId, today, dDay, spareQuantity)

        return queryFactory
            .select(item.count())
            .from(item)
            .where(*predicates.toTypedArray())
            .fetchOne() ?: 0L
    }

    // findItemList와 countItemList가 공유하는 기본 필터 조건. 커서/정렬은 포함하지 않는다.
    private fun basePredicates(
        item: QItem,
        userId: Long,
        today: LocalDate,
        dDay: Int?,
        spareQuantity: Int?,
    ): List<BooleanExpression> = listOfNotNull(
        item.userId.eq(userId),
        item.deletedAt.isNull,
        itemPredicateRepository.filterDday(item, today, dDay),
        itemPredicateRepository.filterSpareQuantity(item, spareQuantity),
    )

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

    // 정렬 키가 같을 때 id로 동률을 깨, 동일 정렬 값을 가진 row가 페이지 경계에서 누락/중복되지 않도록 한다.
    private fun cursorPredicate(item: QItem, order: ItemOrder, cursor: Item): BooleanExpression {
        val cursorId = requireNotNull(cursor.id)

        return when (order) {
            ItemOrder.REPLACEMENT_URGENT -> item.nextReplacementDate.gt(cursor.nextReplacementDate)
                .or(item.nextReplacementDate.eq(cursor.nextReplacementDate).and(item.id.gt(cursorId)))
            ItemOrder.SPARE_LOW -> spareLowCursorPredicate(item, cursor, cursorId)
            ItemOrder.USED_OLD -> item.lastReplacedDate.gt(cursor.lastReplacedDate)
                .or(item.lastReplacedDate.eq(cursor.lastReplacedDate).and(item.id.gt(cursorId)))
            ItemOrder.ITEM_NAME -> item.name.gt(cursor.name)
                .or(item.name.eq(cursor.name).and(item.id.gt(cursorId)))
        }
    }

    /**
     * 여분 적은 순 커서 조건.
     *
     * 미입력(null)은 정렬 마지막 구간이라 숫자 비교로는 이어지지 않는다. NULL과의 비교는 결과가 NULL이라
     * 어떤 행도 통과하지 못해, 커서가 미입력 항목에 닿는 순간 페이징이 끊긴다. 구간을 나눠 처리한다.
     */
    private fun spareLowCursorPredicate(item: QItem, cursor: Item, cursorId: Long): BooleanExpression {
        val cursorQuantity = cursor.quantity
            ?: // 이미 미입력 구간이므로 같은 구간 안에서 보조 키로만 진행한다.
            return item.quantity.isNull
                .and(
                    item.nextReplacementDate.gt(cursor.nextReplacementDate)
                        .or(item.nextReplacementDate.eq(cursor.nextReplacementDate).and(item.id.gt(cursorId))),
                )

        return item.quantity.gt(cursorQuantity)
            // 미입력은 어떤 숫자보다 뒤에 오므로 항상 다음 페이지에 포함된다.
            .or(item.quantity.isNull)
            .or(item.quantity.eq(cursorQuantity).and(item.nextReplacementDate.gt(cursor.nextReplacementDate)))
            .or(
                item.quantity.eq(cursorQuantity)
                    .and(item.nextReplacementDate.eq(cursor.nextReplacementDate))
                    .and(item.id.gt(cursorId)),
            )
    }

    private fun orderSpecifiers(item: QItem, order: ItemOrder): List<OrderSpecifier<*>> = when (order) {
        ItemOrder.REPLACEMENT_URGENT -> listOf(item.nextReplacementDate.asc(), item.id.asc())
        // 미입력은 여분이 적은 것이 아니므로 뒤로 보낸다. DB 기본 NULL 정렬 순서에 기대지 않는다.
        ItemOrder.SPARE_LOW -> listOf(
            item.quantity.asc().nullsLast(),
            item.nextReplacementDate.asc(),
            item.id.asc(),
        )
        ItemOrder.USED_OLD -> listOf(item.lastReplacedDate.asc(), item.id.asc())
        ItemOrder.ITEM_NAME -> listOf(item.name.asc(), item.id.asc())
    }
}
