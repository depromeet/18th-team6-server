package depromeet.hotsix.obrit.category.repository

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    private val testUserId = 1L

    @BeforeEach
    @Transactional
    fun setUp() {
        categoryRepository.saveAll(CategoryFixture.presetCategories())
        categoryRepository.saveAll(CategoryFixture.userCategories(testUserId))
        categoryRepository.save(CategoryFixture.otherUserCategory())
    }

    @AfterEach
    @Transactional
    fun tearDown() {
        categoryRepository.deleteAll()
    }

    @Test
    @Transactional
    fun `findByUserIdWithCursor는_사용자의_카테고리를_반환한다`() {
        // given
        val cursor = 0L
        val pageable = PageRequest.of(0, 20, Sort.by("id").ascending())

        // when
        val result = categoryRepository.findByUserIdWithCursor(testUserId, cursor, pageable)

        // then
        assertEquals(2, result.content.size)
        assertTrue(result.content.all { it.userId == testUserId })
        assertTrue(result.content.none { it.userId == 999L })
    }

    @Test
    @Transactional
    fun `findByUserIdWithCursor는_삭제된_카테고리를_제외한다`() {
        // given
        val cursor = 0L
        val pageable = PageRequest.of(0, 20, Sort.by("id").ascending())

        // when
        val result = categoryRepository.findByUserIdWithCursor(testUserId, cursor, pageable)

        // then: 모두 deletedAt이 null이어야 함
        assertTrue(result.content.all { it.deletedAt == null })
    }

    @Test
    @Transactional
    fun `findByUserIdWithCursor는_커서보다_큰_id의_데이터만_반환한다`() {
        // given
        val allCategories = categoryRepository.findByUserIdWithCursor(
            testUserId,
            0L,
            PageRequest.of(0, 20, Sort.by("id").ascending()),
        ).content
        val firstCategoryId = allCategories[0].id!!

        // when
        val result = categoryRepository.findByUserIdWithCursor(
            testUserId,
            firstCategoryId,
            PageRequest.of(0, 20, Sort.by("id").ascending()),
        )

        // then
        assertTrue(result.content.all { it.id!! > firstCategoryId })
    }

    @Test
    @Transactional
    fun `findByUserIdIsNullAndDeletedAtIsNull은_userId가_null이고_deletedAt이_null인_데이터만_반환한다`() {
        // given
        val pageable = PageRequest.of(0, 20, Sort.by("id").ascending())

        // when
        val result = categoryRepository.findByUserIdIsNullAndDeletedAtIsNull(pageable)

        // then
        assertEquals(3, result.content.size)
        assertTrue(result.content.all { it.userId == null && it.deletedAt == null })
    }

    @Test
    @Transactional
    fun `findByUserIdIsNullAndDeletedAtIsNull은_페이지네이션을_지원한다`() {
        // given
        val pageable = PageRequest.of(0, 2, Sort.by("id").ascending())

        // when
        val result = categoryRepository.findByUserIdIsNullAndDeletedAtIsNull(pageable)

        // then
        assertEquals(2, result.content.size)
        assertTrue(result.hasNext())
    }
}
