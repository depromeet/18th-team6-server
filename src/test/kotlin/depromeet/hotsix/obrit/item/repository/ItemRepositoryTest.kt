package depromeet.hotsix.obrit.item.repository

import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.entity.ItemOrder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemRepositoryTest {

    @Autowired
    private lateinit var itemRepository: ItemRepository

    // countItemList가 동일 필터 기준으로 페이지 크기와 무관하게 전체 개수를 반환하는지 확인한다.
    @Test
    fun `countItemList는_필터_적용_후_전체_개수를_반환한다`() {
        val today = LocalDate.of(2026, 6, 2)
        itemRepository.saveAll(
            listOf(
                item(name = "여분 없음", userId = 1L, quantity = 0, today = today),
                item(name = "기준 수량", userId = 1L, quantity = 2, today = today),
                item(name = "여분 많음", userId = 1L, quantity = 3, today = today),
                item(name = "다른 사용자", userId = 2L, quantity = 1, today = today),
            ),
        )

        val total = itemRepository.countItemList(
            userId = 1L,
            dDay = null,
            spareQuantity = null,
            today = today,
        )
        val filtered = itemRepository.countItemList(
            userId = 1L,
            dDay = null,
            spareQuantity = 2,
            today = today,
        )

        assertEquals(3L, total)
        assertEquals(2L, filtered)
    }

    // 다른 사용자의 아이템은 카운트에 포함되지 않는지 확인한다.
    @Test
    fun `countItemList는_다른_사용자의_아이템을_제외한다`() {
        val today = LocalDate.of(2026, 6, 2)
        itemRepository.saveAll(
            listOf(
                item(name = "내것1", userId = 1L, quantity = 1, today = today),
                item(name = "내것2", userId = 1L, quantity = 1, today = today),
                item(name = "남의것", userId = 2L, quantity = 1, today = today),
            ),
        )

        val count = itemRepository.countItemList(
            userId = 1L,
            dDay = null,
            spareQuantity = null,
            today = today,
        )

        assertEquals(2L, count)
    }

    // 여분 수량 필터가 기준값 이하인 아이템만 반환하는지 확인한다.
    @Test
    fun `findItemList는_spareQuantity_이하인_아이템만_반환한다`() {
        val today = LocalDate.of(2026, 6, 2)
        itemRepository.saveAll(
            listOf(
                item(name = "여분 없음", userId = 1L, quantity = 0, today = today),
                item(name = "기준 수량", userId = 1L, quantity = 2, today = today),
                item(name = "여분 많음", userId = 1L, quantity = 3, today = today),
                item(name = "다른 사용자", userId = 2L, quantity = 1, today = today),
            ),
        )

        val result = itemRepository.findItemList(
            userId = 1L,
            order = ItemOrder.SPARE_LOW,
            dDay = null,
            spareQuantity = 2,
            cursor = null,
            today = today,
            size = 10,
        )

        assertEquals(listOf(0, 2), result.map { it.quantity })
        assertEquals(listOf("여분 없음", "기준 수량"), result.map { it.name })
    }

    private fun item(name: String, userId: Long, quantity: Int, today: LocalDate): Item = Item(
        userId = userId,
        categoryId = 1L,
        name = name,
        quantity = quantity,
        replacementIntervalDays = 30,
        lastReplacedDate = today.minusDays(10),
        nextReplacementDate = today.plusDays(20),
    )
}
