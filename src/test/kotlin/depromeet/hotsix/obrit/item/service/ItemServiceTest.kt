package depromeet.hotsix.obrit.item.service

import depromeet.hotsix.obrit.category.entity.CategoryIcon
import depromeet.hotsix.obrit.category.repository.CategoryFixture
import depromeet.hotsix.obrit.category.repository.CategoryIconRepository
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.entity.ItemDetailStatus
import depromeet.hotsix.obrit.item.entity.ItemReplacementHistory
import depromeet.hotsix.obrit.item.repository.ItemReplacementHistoryRepository
import depromeet.hotsix.obrit.item.repository.ItemRepository
import depromeet.hotsix.obrit.user.entity.UserFixture
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
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
class ItemServiceTest {

    @Autowired
    private lateinit var itemService: ItemService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var categoryIconRepository: CategoryIconRepository

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var itemReplacementHistoryRepository: ItemReplacementHistoryRepository

    private val today = LocalDate.now()
    private var userId: Long = 0
    private var itemId: Long = 0

    @BeforeEach
    fun setUp() {
        val user = userRepository.save(UserFixture.user(id = null))
        userId = requireNotNull(user.id)
        val icon = categoryIconRepository.save(
            CategoryIcon(
                name = "칫솔",
                url = "https://cdn.obrit.app/icons/toothbrush.png",
            ),
        )
        val category = categoryRepository.save(
            CategoryFixture.presetCategory(
                name = "칫솔",
                iconId = icon.id,
                defaultReplacementIntervalDays = 30,
            ),
        )
        val item = itemRepository.save(
            Item(
                userId = userId,
                categoryId = requireNotNull(category.id),
                name = "회사용 칫솔",
                quantity = 0,
                replacementIntervalDays = 30,
                lastReplacedDate = today.minusDays(31),
                nextReplacementDate = today.minusDays(1),
            ),
        )
        itemId = requireNotNull(item.id)

        val replacementDates = listOf(143, 116, 88, 48)
            .map { today.minusDays(it.toLong()) }
        itemReplacementHistoryRepository.saveAll(
            replacementDates.map { replacedDate ->
                ItemReplacementHistory(item = item, replacedDate = replacedDate)
            },
        )
    }

    @Test
    fun `소모품 상세를 조회하면 카테고리와 교체 상태와 최근 교체 기록을 반환한다`() {
        val result = itemService.getItemDetail(userId, itemId)

        assertEquals(itemId, result.id)
        assertEquals("회사용 칫솔", result.name)
        assertEquals("칫솔", result.category.name)
        assertEquals("https://cdn.obrit.app/icons/toothbrush.png", result.iconUrl)
        assertEquals(ItemDetailStatus.DANGER, result.status)
        assertEquals(-1, result.dday)
        assertEquals("D+1", result.ddayLabel)
        assertEquals(0, result.spareCount)
        assertEquals(today.minusDays(31), result.lastReplacedDate)
        assertEquals(today.minusDays(1), result.nextReplacementDate)
        assertEquals(31, result.usedDays)
        assertEquals(31.2, result.myAverageCycleDays)
        assertEquals(30, result.recommendedCycleDays)
        assertEquals(103.3, result.progressPercentage)
        assertEquals(5, result.recentReplacements.size)
        assertEquals(listOf(30, 27, 28, 40, 31), result.recentReplacements.map { it.cycleDays })
        assertEquals(false, result.recentReplacements.first().isCurrent)
        assertEquals(true, result.recentReplacements.last().isCurrent)
    }
}
