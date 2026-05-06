package depromeet.hotsix.obrit.category.service

import depromeet.hotsix.obrit.category.repository.CategoryFixture
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.user.entity.UserFixture
import depromeet.hotsix.obrit.user.repository.UserRepository
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
class CategoryServiceTest {

    @Autowired
    private lateinit var categoryService: CategoryService

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private var user1Id: Long = 0L
    private var user2Id: Long = 0L

    @BeforeEach
    @Transactional
    fun setUp() {
        // 사용자 생성
        val users = userRepository.saveAll(
            listOf(
                UserFixture.user(id = null, name = "사용자1"),
                UserFixture.user(id = null, name = "사용자2"),
            ),
        )
        user1Id = users[0].id!!
        user2Id = users[1].id!!

        // 기본 제공 카테고리 3개
        categoryRepository.saveAll(CategoryFixture.presetCategories())

        // 사용자1 등록 카테고리 2개
        categoryRepository.saveAll(CategoryFixture.userCategories(user1Id))

        // 사용자2 등록 카테고리 2개
        categoryRepository.saveAll(CategoryFixture.userCategories(user2Id))
    }

    @Test
    @Transactional
    fun `사용자는_본인이_등록한_종류와_기본제공_종류만_조회할_수_있다`() {
        // when
        val result = categoryService.listAllAccessibleCategories(user1Id)

        // then: 기본 제공(3개) + 사용자1 등록(2개) = 5개
        assertEquals(5, result.size)

        // 기본 제공 카테고리는 preset = true
        val presetCount = result.count { it.preset }
        assertEquals(3, presetCount)

        // 사용자1 등록 카테고리는 preset = false
        val user1CustomCount = result.count { !it.preset }
        assertEquals(2, user1CustomCount)
    }

    @Test
    @Transactional
    fun `다른_사용자가_등록한_종류는_조회되지_않는다`() {
        // when
        val result = categoryService.listAllAccessibleCategories(user1Id)

        // then: 사용자2의 카테고리는 name이 "커스텀2"와 같지 않음
        assertTrue(result.none { it.name == "다른사용자카테고리" })
    }
}
