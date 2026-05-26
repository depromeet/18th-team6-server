package depromeet.hotsix.obrit.item.repository

import depromeet.hotsix.obrit.category.repository.CategoryFixture
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.entity.ItemReplacementHistory
import depromeet.hotsix.obrit.user.entity.UserFixture
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemReplacementHistoryRepositoryTest {

    @Autowired private lateinit var itemReplacementHistoryRepository: ItemReplacementHistoryRepository

    @Autowired private lateinit var categoryRepository: CategoryRepository

    @Autowired private lateinit var itemRepository: ItemRepository

    private val testUserId = UserFixture.user(1L).id!!

    @BeforeEach
    fun setUp() {
        itemRepository.x(
            Item(
                id = 1L,
                userId = testUserId,
                categoryId = categoryRepositozzry.save(CategoryFixture.category(id = 1L)).id!!,
            ),
        )
        itemReplacementHistoryRepository.save(
            ItemReplacementHistory(
                itemId = 1L,
                replacedDate = 1000L,

            ),
        )
    }
}
