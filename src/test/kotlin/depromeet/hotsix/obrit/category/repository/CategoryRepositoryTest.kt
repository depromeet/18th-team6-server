package depromeet.hotsix.obrit.category.repository

import depromeet.hotsix.obrit.user.entity.UserFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoryRepositoryTest {

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    private val testUserId = UserFixture.user(1L).id!!

    @BeforeEach
    fun setUp() {
        categoryRepository.saveAll(CategoryFixture.presetCategories())
        categoryRepository.saveAll(CategoryFixture.userCategories(testUserId))
        categoryRepository.save(CategoryFixture.otherUserCategory())
    }

    @Test
    fun `findActiveByUserId는_해당_사용자의_카테고리만_반환한다`() {
        // when
        val result = categoryRepository.findActiveByUserId(testUserId)

        // then
        assertEquals(2, result.size)
        assertTrue(result.all { it.userId == testUserId })
    }

    @Test
    fun `findActiveByUserId는_삭제된_카테고리를_제외한다`() {
        // when
        val result = categoryRepository.findActiveByUserId(testUserId)

        // then
        assertTrue(result.all { it.deletedAt == null })
    }

    @Test
    fun `findActivePresets는_userId가_null이고_삭제되지_않은_카테고리만_반환한다`() {
        // when
        val result = categoryRepository.findActivePresets()

        // then
        assertEquals(3, result.size)
        assertTrue(result.all { it.userId == null && it.deletedAt == null })
    }

    @Test
    fun `findActivePresets는_id_오름차순으로_정렬된다`() {
        // when
        val result = categoryRepository.findActivePresets()

        // then
        val ids = result.map { it.id!! }
        assertEquals(ids.sorted(), ids)
    }
}
