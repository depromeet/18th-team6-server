package depromeet.hotsix.obrit.item.repository.predicate

import com.querydsl.core.types.dsl.BooleanExpression
import depromeet.hotsix.obrit.item.entity.QItem
import org.springframework.stereotype.Component
import java.time.LocalDate

// Item 조회의 QueryDSL 필터 생성을 담당하는 객체
@Component
class ItemPredicateRepository {

    // "오늘 + dDay일 이내에 교체가 필요한 아이템"으로 좁히는 필터. dDay가 null이면 필터를 적용하지 않는다.
    fun filterDday(item: QItem, today: LocalDate, dDay: Int?): BooleanExpression? {
        if (dDay == null) {
            return null
        }

        return item.nextReplacementDate.loe(today.plusDays(dDay.toLong()))
    }

    // 여분 수량이 spareQuantity 이상인 아이템만 남기는 필터. spareQuantity가 null이면 적용하지 않는다.
    fun filterSpareQuantity(item: QItem, spareQuantity: Int?): BooleanExpression? {
        if (spareQuantity == null) {
            return null
        }

        return item.quantity.goe(spareQuantity)
    }
}
