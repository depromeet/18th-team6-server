package depromeet.hotsix.obrit.admin.service

import depromeet.hotsix.obrit.admin.dto.AdminIconForm
import depromeet.hotsix.obrit.admin.dto.AdminItemForm
import depromeet.hotsix.obrit.category.entity.Category
import depromeet.hotsix.obrit.category.entity.CategoryIcon
import depromeet.hotsix.obrit.category.repository.CategoryIconRepository
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.item.entity.Item
import depromeet.hotsix.obrit.item.repository.ItemRepository
import depromeet.hotsix.obrit.user.entity.User
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminBackofficeServiceTest {

    @Autowired
    private lateinit var adminBackofficeService: AdminBackofficeService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var categoryIconRepository: CategoryIconRepository

    @Autowired
    private lateinit var itemRepository: ItemRepository

    private var userId: Long = 0L
    private var iconId: Long = 0L
    private var presetCategoryId: Long = 0L
    private var customCategoryId: Long = 0L

    @BeforeEach
    fun setUp() {
        val user = userRepository.save(User(uuid = "550e8400-e29b-41d4-a716-446655440000", name = "active-user"))
        val deletedUser = userRepository.save(
            User(
                uuid = "550e8400-e29b-41d4-a716-446655440001",
                name = "deleted-user",
            ),
        )
        deletedUser.softDelete()
        userId = requireNotNull(user.id)

        val icon = categoryIconRepository.save(CategoryIcon(name = "default", url = "/icons/default.png"))
        iconId = icon.id
        val preset = categoryRepository.save(
            Category(
                userId = null,
                name = "Preset",
                iconId = icon.id,
                defaultReplacementIntervalDays = 30,
            ),
        )
        val custom = categoryRepository.save(
            Category(
                userId = userId,
                name = "Custom",
                iconId = icon.id,
                defaultReplacementIntervalDays = 7,
            ),
        )
        presetCategoryId = requireNotNull(preset.id)
        customCategoryId = requireNotNull(custom.id)

        itemRepository.save(
            Item(
                userId = userId,
                categoryId = presetCategoryId,
                name = "Preset item",
                quantity = 1,
                replacementIntervalDays = 30,
                lastReplacedDate = LocalDate.of(2026, 5, 1),
                nextReplacementDate = LocalDate.of(2026, 5, 31),
            ),
        )
        val deletedItem = itemRepository.save(
            Item(
                userId = userId,
                categoryId = customCategoryId,
                name = "Deleted item",
                quantity = 2,
                replacementIntervalDays = 7,
                lastReplacedDate = LocalDate.of(2026, 5, 1),
                nextReplacementDate = LocalDate.of(2026, 5, 8),
            ),
        )
        deletedItem.softDelete()
    }

    @Test
    fun `테이블 조회는 기본적으로 활성 데이터만 보여주고 토글 시 삭제 데이터도 포함한다`() {
        val activeUsers = adminBackofficeService.listUsers(includeDeleted = false)
        val allUsers = adminBackofficeService.listUsers(includeDeleted = true)
        val activeItems = adminBackofficeService.listItems(includeDeleted = false)
        val allItems = adminBackofficeService.listItems(includeDeleted = true)

        assertEquals(listOf("active-user"), activeUsers.map { it.name })
        assertEquals(listOf("active-user", "deleted-user"), allUsers.map { it.name }.sorted())
        assertEquals(listOf("Preset item"), activeItems.map { it.name })
        assertEquals(listOf("Deleted item", "Preset item"), allItems.map { it.name }.sorted())
    }

    @Test
    fun `사용자 삭제는 사용자 아이템과 사용자 카테고리를 함께 소프트 삭제한다`() {
        adminBackofficeService.deleteUser(userId)

        val deletedUser = userRepository.findById(userId).orElseThrow()
        val affectedItems = itemRepository.findAll().filter { it.userId == userId }
        val customCategory = categoryRepository.findById(customCategoryId).orElseThrow()
        val presetCategory = categoryRepository.findById(presetCategoryId).orElseThrow()

        assertNotNull(deletedUser.deletedAt)
        assertTrue(affectedItems.all { it.deletedAt != null })
        assertNotNull(customCategory.deletedAt)
        assertEquals(null, presetCategory.deletedAt)
    }

    @Test
    fun `프리셋 카테고리 삭제는 연결된 아이템을 함께 소프트 삭제한다`() {
        adminBackofficeService.deleteCategory(presetCategoryId)

        val deletedCategory = categoryRepository.findById(presetCategoryId).orElseThrow()
        val affectedItems = itemRepository.findAll().filter { it.categoryId == presetCategoryId }

        assertNotNull(deletedCategory.deletedAt)
        assertTrue(affectedItems.all { it.deletedAt != null })
    }

    @Test
    fun `아이콘은 테이블로 조회하고 생성 수정 삭제할 수 있다`() {
        adminBackofficeService.createIcon(AdminIconForm(name = "new", url = "/icons/new.png"))
        val created = adminBackofficeService.listIcons(includeDeleted = false)
            .first { it.name == "new" }

        adminBackofficeService.updateIcon(created.id, AdminIconForm(name = "updated", url = "/icons/updated.png"))
        val updated = adminBackofficeService.getIcon(created.id)

        adminBackofficeService.deleteIcon(created.id)
        val activeIcons = adminBackofficeService.listIcons(includeDeleted = false)
        val allIcons = adminBackofficeService.listIcons(includeDeleted = true)

        assertEquals("updated", updated.name)
        assertEquals("/icons/updated.png", updated.url)
        assertTrue(activeIcons.none { it.id == created.id })
        assertTrue(allIcons.any { it.id == created.id && it.deletedAt != null })
    }

    @Test
    fun `사용 중인 아이콘은 삭제할 수 없다`() {
        assertFailsWith<RuntimeException> {
            adminBackofficeService.deleteIcon(iconId)
        }

        val icon = adminBackofficeService.getIcon(iconId)

        assertEquals(null, icon.deletedAt)
    }

    @Test
    fun `아이템은 다른 사용자의 커스텀 카테고리에 연결할 수 없다`() {
        val otherUser = userRepository.save(User(uuid = "550e8400-e29b-41d4-a716-446655440099", name = "other-user"))

        assertFailsWith<RuntimeException> {
            adminBackofficeService.createItem(
                AdminItemForm(
                    userId = requireNotNull(otherUser.id),
                    categoryId = customCategoryId,
                    name = "Invalid item",
                    count = 1,
                    lastReplacedDate = LocalDate.of(2026, 5, 1),
                    replacementIntervalDays = 7,
                ),
            )
        }
    }
}
