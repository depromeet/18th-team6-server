package depromeet.hotsix.obrit.home.service

import depromeet.hotsix.obrit.home.entity.ItemBucket
import depromeet.hotsix.obrit.home.entity.ItemSnapshot
import depromeet.hotsix.obrit.home.entity.OverallStatus
import depromeet.hotsix.obrit.item.entity.ItemStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HomeStatusCalculatorServiceTest {

    private val calculator = HomeStatusCalculatorService()
    private val today = LocalDate.of(2026, 5, 9)

    @Test
    fun `빈 목록은 기본 상태와 기본 점수를 반환한다`() {
        val result = calculator.calculate(today, emptyList())

        assertEquals(ItemStatus.GOOD, result.overallStatus.replacement)
        assertEquals(ItemStatus.GOOD, result.overallStatus.spare)
        assertEquals(OverallStatus.PERFECT, result.overallStatus.overall)
        assertEquals(0, result.myStatusSummary.totalCount)
        assertEquals(0, result.myStatusSummary.needReplaceCount)
        assertEquals(45.0, result.myStatusSummary.score)
        assertEquals(45.0, result.myStatusSummary.averageScore)
        assertEquals(6, result.itemBuckets.size)
        assertEquals(0, result.itemBuckets.sumOf { it.count })
    }

    @Test
    fun `위험 비율이 0퍼센트이면 평균이 1점대여도 교체 상태는 양호다`() {
        val items = List(7) { item(id = it.toLong(), daysUntil = 10, quantity = 3) } +
            List(3) { item(id = (it + 10).toLong(), daysUntil = 3, quantity = 3) }

        val result = calculator.calculate(today, items)

        assertEquals(ItemStatus.GOOD, result.overallStatus.replacement)
        assertEquals(ItemStatus.GOOD, result.overallStatus.spare)
        assertEquals(OverallStatus.PERFECT, result.overallStatus.overall)
    }

    @Test
    fun `교체 필요 수는 overdue만 세고 warn은 제외한다`() {
        val items = listOf(
            item(id = 1, daysUntil = 0, quantity = 1),
            item(id = 2, daysUntil = 3, quantity = 1),
            item(id = 3, daysUntil = 4, quantity = 1),
        )

        val result = calculator.calculate(today, items)

        assertEquals(1, result.myStatusSummary.needReplaceCount)
        assertEquals(45.0, result.myStatusSummary.averageScore)
    }

    @Test
    fun `여섯 개 버킷을 우선순위 순서로 반환한다`() {
        val items = listOf(
            item(id = 1, daysUntil = -1, quantity = 0),
            item(id = 2, daysUntil = 3, quantity = 0),
            item(id = 3, daysUntil = 0, quantity = 5),
            item(id = 4, daysUntil = 3, quantity = 5),
            item(id = 5, daysUntil = 4, quantity = 0),
            item(id = 6, daysUntil = 4, quantity = 5),
        )

        val result = calculator.calculate(today, items)

        assertEquals(ItemBucket.entries.sortedBy { it.priority }, result.itemBuckets.map { it.bucket })
        assertEquals(List(6) { 1 }, result.itemBuckets.map { it.count })
    }

    private fun item(id: Long, daysUntil: Long, quantity: Int): ItemSnapshot = ItemSnapshot(
        id = id,
        name = "item-$id",
        nextReplacementDate = today.plusDays(daysUntil),
        quantity = quantity,
    )
}
