package depromeet.hotsix.obrit.category.service

import depromeet.hotsix.obrit.category.dto.request.CreateCategoryRequest
import depromeet.hotsix.obrit.category.entity.CategoryGroup
import depromeet.hotsix.obrit.category.entity.CategoryGroupType
import depromeet.hotsix.obrit.category.repository.CategoryFixture
import depromeet.hotsix.obrit.category.repository.CategoryGroupRepository
import depromeet.hotsix.obrit.category.repository.CategoryIconFixture
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.user.entity.UserFixture
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoryGroupServiceTest {

    @Autowired
    private lateinit var categoryService: CategoryService

    @Autowired
    private lateinit var categoryQueryService: CategoryQueryService

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var categoryGroupRepository: CategoryGroupRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private var userId: Long = 0L

    @BeforeEach
    fun setUp() {
        CategoryIconFixture.ensureDefaultIcon(jdbcTemplate)
        userId = userRepository.save(UserFixture.user(id = null, name = "사용자1")).id!!
    }

    @Test
    fun `카테고리 목록은 삭제된 것을 빼고 노출 순서대로 준다`() {
        val user = saveGroup("사용자", CategoryGroupType.USER, 1000)
        val kitchen = saveGroup("주방", CategoryGroupType.PRESET, 2)
        val bathroom = saveGroup("욕실", CategoryGroupType.PRESET, 1)
        saveGroup("세탁실", CategoryGroupType.PRESET, 3).softDelete()

        val result = categoryQueryService.listCategoryGroups()

        assertEquals(listOf(bathroom.id, kitchen.id, user.id), result.map { it.categoryGroupId })
        assertEquals(listOf("욕실", "주방", "사용자"), result.map { it.name })
        assertEquals(CategoryGroupType.USER, result.last().type)
        assertEquals(1000, result.last().displayOrder)
    }

    @Test
    fun `소모품 생성 API로 만든 사용자 소모품은 사용자 카테고리에 속한다`() {
        val userGroup = saveGroup("사용자", CategoryGroupType.USER, 1000)

        val result = categoryService.createCategory(userId, CreateCategoryRequest(name = "수건"))

        assertEquals(userGroup.id, result.categoryGroupId)
        assertEquals("사용자", result.categoryGroupName)
        assertEquals(userGroup.id, categoryRepository.findById(result.categoryId).orElseThrow().categoryGroupId)
    }

    @Test
    fun `내 소모품 등록 중 새로 만든 사용자 소모품도 사용자 카테고리에 속한다`() {
        val userGroup = saveGroup("사용자", CategoryGroupType.USER, 1000)

        val categoryId = categoryService.getOrCreateUserCategoryId(userId, "수세미")

        assertEquals(userGroup.id, categoryRepository.findById(categoryId).orElseThrow().categoryGroupId)
    }

    @Test
    fun `사용자 카테고리가 없으면 소모품은 만들고 카테고리는 비워 둔다`() {
        saveGroup("욕실", CategoryGroupType.PRESET, 1)

        val createdId = categoryService.createCategory(userId, CreateCategoryRequest(name = "수건")).categoryId
        val registeredId = categoryService.getOrCreateUserCategoryId(userId, "수세미")

        assertNull(categoryRepository.findById(createdId).orElseThrow().categoryGroupId)
        assertNull(categoryRepository.findById(registeredId).orElseThrow().categoryGroupId)
        assertNull(categoryQueryService.listAllAccessibleCategories(userId).first().categoryGroupName)
    }

    // 기본 종류와 사용자 종류의 그룹 이름을 함께 반환하고 미분류 종류는 null로 둔다.
    @Test
    fun `소모품 전체 조회는 각 소모품의 소속 그룹 ID와 이름을 준다`() {
        val bathroom = saveGroup("욕실", CategoryGroupType.PRESET, 1)
        val userGroup = saveGroup("사용자", CategoryGroupType.USER, 1000)
        val toothbrush = CategoryFixture.presetCategory(name = "칫솔").apply { categoryGroupId = bathroom.id }
        categoryRepository.saveAll(listOf(toothbrush, CategoryFixture.presetCategory(name = "면도기")))
        categoryService.createCategory(userId, CreateCategoryRequest(name = "수건"))

        val groupByName = categoryQueryService.listAllAccessibleCategories(userId)
            .associate { it.name to (it.categoryGroupId to it.categoryGroupName) }

        assertEquals(
            mapOf("칫솔" to (bathroom.id to "욕실"), "면도기" to (null to null), "수건" to (userGroup.id to "사용자")),
            groupByName,
        )
    }

    // 삭제되거나 존재하지 않는 그룹의 이름은 노출하지 않고 기존 그룹 ID를 유지한다.
    @Test
    fun `소속 그룹이 삭제되거나 없으면 그룹 이름은 null이다`() {
        val deletedGroup = saveGroup("욕실", CategoryGroupType.PRESET, 1).apply { softDelete() }
        categoryRepository.saveAll(
            listOf(
                CategoryFixture.presetCategory(name = "칫솔").apply { categoryGroupId = deletedGroup.id },
                CategoryFixture.presetCategory(name = "수세미").apply { categoryGroupId = Long.MAX_VALUE },
            ),
        )

        val groupByName = categoryQueryService.listAllAccessibleCategories(userId)
            .associate { it.name to (it.categoryGroupId to it.categoryGroupName) }

        assertEquals(mapOf("칫솔" to (deletedGroup.id to null), "수세미" to (Long.MAX_VALUE to null)), groupByName)
    }

    private fun saveGroup(name: String, type: CategoryGroupType, displayOrder: Int): CategoryGroup =
        categoryGroupRepository.save(CategoryGroup(name = name, type = type, displayOrder = displayOrder))
}
