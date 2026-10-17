package depromeet.hotsix.obrit.item

import depromeet.hotsix.obrit.category.entity.CategoryIcon
import depromeet.hotsix.obrit.category.repository.CategoryFixture
import depromeet.hotsix.obrit.category.repository.CategoryIconRepository
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.item.dto.CreateItemRequest
import depromeet.hotsix.obrit.item.service.ItemService
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
            CategoryIcon(name = "칫솔", key = "", url = "https://cdn.obrit.app/icons/toothbrush.png"),
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

    @Test
    fun `등록하면_오늘을_마지막_교체일로_쓴다`() {
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
