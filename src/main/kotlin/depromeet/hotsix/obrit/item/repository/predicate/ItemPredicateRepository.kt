package depromeet.hotsix.obrit.item.repository.predicate

import com.querydsl.core.types.dsl.BooleanExpression
import depromeet.hotsix.obrit.item.entity.QItem
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ItemPredicateRepository {

    fun filterDday(item: QItem, today: LocalDate, dDay: Int?): BooleanExpression? {
        if (dDay == null) {
            return null
        }

        return item.nextReplacementDate.loe(today.plusDays(dDay.toLong()))
    }

    fun filterSpareQuantity(item: QItem, spareQuantity: Int?): BooleanExpression? {
        if (spareQuantity == null) {
            return null
        }

        return item.quantity.goe(spareQuantity)
    }
}
