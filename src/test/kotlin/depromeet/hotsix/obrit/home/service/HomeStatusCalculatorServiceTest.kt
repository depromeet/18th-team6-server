package depromeet.hotsix.obrit.home.service

import depromeet.hotsix.obrit.home.entity.HomeRiskBucket
import depromeet.hotsix.obrit.home.entity.OverallStatus
import depromeet.hotsix.obrit.item.entity.ItemSnapshot
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
        assertEquals(listOf(HomeRiskBucket.DANGER, HomeRiskBucket.WARNING), result.itemBuckets.map { it.bucket })
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
    fun `위험_경고 두 버킷을 항상 반환하고 양호 아이템은 제외한다`() {
        val items = listOf(
            item(id = 1, daysUntil = -1, quantity = 0),
            item(id = 2, daysUntil = 3, quantity = 0),
            item(id = 3, daysUntil = 0, quantity = 5),
            item(id = 4, daysUntil = 3, quantity = 5),
            item(id = 5, daysUntil = 4, quantity = 0),
            item(id = 6, daysUntil = 4, quantity = 5),
        )

        val result = calculator.calculate(today, items)

        assertEquals(listOf(HomeRiskBucket.DANGER, HomeRiskBucket.WARNING), result.itemBuckets.map { it.bucket })
        assertEquals(listOf(3, 2), result.itemBuckets.map { it.count })

        val dangerIds = result.itemBuckets[0].items.map { it.id }
        val warningIds = result.itemBuckets[1].items.map { it.id }
        assertEquals(listOf(1L, 3L, 2L), dangerIds)
        assertEquals(listOf(4L, 5L), warningIds)
    }

    @Test
    fun `버킷 내 아이템은 교체 D-day가 가장 지난 순으로 정렬된다`() {
        val items = listOf(
            item(id = 1, daysUntil = 0, quantity = 0),
            item(id = 2, daysUntil = -5, quantity = 0),
            item(id = 3, daysUntil = -2, quantity = 0),
        )

        val result = calculator.calculate(today, items)

        val dangerItems = result.itemBuckets.first { it.bucket == HomeRiskBucket.DANGER }.items
        assertEquals(listOf(2L, 3L, 1L), dangerItems.map { it.id })
    }

    private fun item(id: Long, daysUntil: Long, quantity: Int): ItemSnapshot = ItemSnapshot(
        id = id,
        name = "item-$id",
        categoryId = 100L,
        nextReplacementDate = today.plusDays(daysUntil),
        quantity = quantity,
    )
}
