package depromeet.hotsix.obrit.item

import depromeet.hotsix.obrit.category.entity.CategoryIcon
import depromeet.hotsix.obrit.category.repository.CategoryFixture
import depromeet.hotsix.obrit.category.repository.CategoryIconRepository
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.item.dto.CreateItemRequest
import depromeet.hotsix.obrit.item.entity.LastReplacementPeriod
import depromeet.hotsix.obrit.item.service.ItemService
import depromeet.hotsix.obrit.user.entity.UserFixture
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemCreateAcceptanceTest {

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

    private var userId: Long = 0
    private var categoryId: Long = 0

    @BeforeEach
    fun setUp() {
        val user = userRepository.save(UserFixture.user(id = null))
        userId = requireNotNull(user.id)
        val icon = categoryIconRepository.save(
            CategoryIcon(name = "칫솔", url = "https://cdn.obrit.app/icons/toothbrush.png"),
        )
        val category = categoryRepository.save(
            CategoryFixture.presetCategory(
                name = "칫솔",
                iconId = icon.id,
                defaultReplacementIntervalDays = 30,
            ),
        )
        categoryId = requireNotNull(category.id)
    }

    @ParameterizedTest
    @EnumSource(LastReplacementPeriod::class)
    fun `교체_시기_선택지는_기한의_평균치를_교체일자로_사용한다`(period: LastReplacementPeriod) {
        val baseDate = LocalDate.now(clock)
        val expectedDaysAgo = when (period) {
            LastReplacementPeriod.WITHIN_WEEK -> 4L
            LastReplacementPeriod.WITHIN_MONTH -> 21L
            LastReplacementPeriod.WITHIN_THREE_MONTHS -> 45L
            LastReplacementPeriod.OVER_THREE_MONTHS -> 90L
        }

        val result = itemService.createItem(
            userId,
            CreateItemRequest(
                categoryId = categoryId,
                name = "테스트 소모품",
                spareQuantity = 1,
                lastReplacementPeriod = period,
            ),
        )

        assertEquals(baseDate.minusDays(expectedDaysAgo), result.lastReplacedDate)
        assertEquals(baseDate.minusDays(expectedDaysAgo).plusDays(30), result.nextReplacementDate)
    }

    @Test
    fun `교체_시기를_선택하지_않으면_오늘_날짜로_설정된다`() {
        val baseDate = LocalDate.now(clock)
        val result = itemService.createItem(
            userId,
            CreateItemRequest(
                categoryId = categoryId,
                name = "테스트 소모품",
                spareQuantity = 1,
            ),
        )

        assertEquals(baseDate, result.lastReplacedDate)
        assertEquals(baseDate.plusDays(30), result.nextReplacementDate)
    }
}
