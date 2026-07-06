package depromeet.hotsix.obrit.item.service

import depromeet.hotsix.obrit.category.entity.CategoryIcon
import depromeet.hotsix.obrit.category.repository.CategoryFixture
import depromeet.hotsix.obrit.category.repository.CategoryIconRepository
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.item.dto.CreateReplacementRequest
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
import java.time.Clock
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemServiceTest {

    @Autowired
    private lateinit var itemService: ItemService

    @Autowired
    private lateinit var clock: Clock

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

    private val today: LocalDate
        get() = LocalDate.now(clock)
    private var userId: Long = 0
    private var itemId: Long = 0

    @BeforeEach
    fun setUp() {
        val user = userRepository.save(UserFixture.user(id = null))
        userId = requireNotNull(user.id)
        val icon = categoryIconRepository.save(
            CategoryIcon(
                name = "칫솔",
                key = "icons/toothbrush.png",
                url = "https://legacy.example.com/icons/toothbrush.png",
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

        assertEquals(itemId, result.itemId)
        assertEquals("회사용 칫솔", result.name)
        assertEquals("칫솔", result.category.name)
        assertEquals("icons/toothbrush.png", result.iconUrl)
        assertEquals(ItemDetailStatus.DANGER, result.status)
        assertEquals(-1, result.dday)
        assertEquals("D+1", result.ddayLabel)
        assertEquals(0, result.spareQuantity)
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

    @Test
    fun `동일한 교체일 이력이 있어도 기록별 주기를 덮어쓰지 않는다`() {
        val item = itemRepository.getReferenceById(itemId)
        itemReplacementHistoryRepository.save(
            ItemReplacementHistory(item = item, replacedDate = today.minusDays(88)),
        )

        val result = itemService.getItemDetail(userId, itemId)

        assertEquals(listOf(27, 28, 0, 40, 31), result.recentReplacements.map { it.cycleDays })
    }

    @Test
    fun `현재 교체일과 같은 이력이 여러 건이어도 현재 사용중 기록은 하나만 표시한다`() {
        val item = itemRepository.getReferenceById(itemId)
        itemReplacementHistoryRepository.saveAll(
            listOf(
                ItemReplacementHistory(item = item, replacedDate = today.minusDays(31)),
                ItemReplacementHistory(item = item, replacedDate = today.minusDays(31)),
            ),
        )

        val result = itemService.getItemDetail(userId, itemId)

        assertEquals(1, result.recentReplacements.count { it.isCurrent })
        assertEquals(true, result.recentReplacements.last().isCurrent)
    }

    @Test
    fun `소모품 교체를 기록하면 여분 수량을 1 차감한다`() {
        val item = itemRepository.getReferenceById(itemId)
        item.updateSpareCount(2)
        val replacedDate = today

        val result = itemService.replaceItem(
            userId = userId,
            itemId = itemId,
            request = CreateReplacementRequest(replacedDate = replacedDate),
        )

        assertEquals(1, result.spareQuantity)
        assertEquals(replacedDate, result.lastReplacedDate)
        assertEquals(replacedDate.plusDays(30), result.nextReplacementDate)
        assertEquals(1, itemRepository.getReferenceById(itemId).quantity)
    }

    @Test
    fun `여분 수량이 0인 소모품을 교체해도 수량은 음수가 되지 않는다`() {
        val result = itemService.replaceItem(
            userId = userId,
            itemId = itemId,
            request = CreateReplacementRequest(replacedDate = today),
        )

        assertEquals(0, result.spareQuantity)
        assertEquals(0, itemRepository.getReferenceById(itemId).quantity)
    }

    @Test
    fun `카테고리 아이콘이 없으면 빈 문자열 대신 예외를 던진다`() {
        val category = categoryRepository.save(
            CategoryFixture.presetCategory(
                name = "아이콘 없는 카테고리",
                iconId = 9999L,
                defaultReplacementIntervalDays = 30,
            ),
        )
        val item = itemRepository.save(
            Item(
                userId = userId,
                categoryId = requireNotNull(category.id),
                name = "아이콘 없는 소모품",
                quantity = 1,
                replacementIntervalDays = 30,
                lastReplacedDate = today,
                nextReplacementDate = today.plusDays(30),
            ),
        )

        val exception = assertFailsWith<ResourceNotFoundException> {
            itemService.getItemDetail(userId, requireNotNull(item.id))
        }

        assertEquals("존재하지 않는 카테고리 아이콘입니다.", exception.message)
    }
}
