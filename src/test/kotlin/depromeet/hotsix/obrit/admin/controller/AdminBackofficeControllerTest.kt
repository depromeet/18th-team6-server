package depromeet.hotsix.obrit.admin.controller

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
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminBackofficeControllerTest {

    @LocalServerPort
    private var port: Int = 0

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var categoryIconRepository: CategoryIconRepository

    @Autowired
    private lateinit var itemRepository: ItemRepository

    private var userId: Long = 0L
    private var categoryId: Long = 0L
    private var itemId: Long = 0L

    @BeforeEach
    fun setUp() {
        itemRepository.deleteAll()
        categoryRepository.deleteAll()
        categoryIconRepository.deleteAll()
        userRepository.deleteAll()

        val user = userRepository.save(
            User(uuid = "admin-controller-user", name = "Admin Controller User"),
        )
        val icon = categoryIconRepository.save(CategoryIcon(name = "default", url = "/icons/default.png"))
        val category = categoryRepository.save(
            Category(
                userId = null,
                name = "Controller Category",
                iconId = icon.id,
                defaultReplacementIntervalDays = 30,
            ),
        )
        val item = itemRepository.save(
            Item(
                userId = requireNotNull(user.id),
                categoryId = requireNotNull(category.id),
                name = "Controller Item",
                quantity = 1,
                replacementIntervalDays = 30,
                lastReplacedDate = LocalDate.of(2026, 5, 1),
                nextReplacementDate = LocalDate.of(2026, 5, 31),
            ),
        )

        userId = requireNotNull(user.id)
        categoryId = requireNotNull(category.id)
        itemId = requireNotNull(item.id)
    }

    @Test
    fun `admin pages require authentication`() {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/admin/users"))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        assertTrue(
            response.statusCode() == HttpStatus.UNAUTHORIZED.value() ||
                response.statusCode() == HttpStatus.FOUND.value(),
        )
    }

    @Test
    fun `admin users page renders with admin credentials`() {
        val credentials = Base64.getEncoder().encodeToString("admin:admin".toByteArray())
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/admin/users"))
            .header("Authorization", "Basic $credentials")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(HttpStatus.OK.value(), response.statusCode())
        assertTrue(response.body().contains("Users"))
    }

    @Test
    fun `admin list pages expose Django admin style add and change navigation`() {
        val users = authenticatedGet("/admin/users")
        val categories = authenticatedGet("/admin/categories")
        val items = authenticatedGet("/admin/items")

        assertEquals(HttpStatus.OK.value(), users.statusCode())
        assertEquals(HttpStatus.OK.value(), categories.statusCode())
        assertEquals(HttpStatus.OK.value(), items.statusCode())

        assertTrue(users.body().contains("Add user"))
        assertTrue(users.body().contains("/admin/users/$userId/change"))
        assertFalse(users.body().contains("Create user"))

        assertTrue(categories.body().contains("Add category"))
        assertTrue(categories.body().contains("/admin/categories/$categoryId/change"))

        assertTrue(items.body().contains("Add item"))
        assertTrue(items.body().contains("/admin/items/$itemId/change"))
    }

    @Test
    fun `admin add and change pages render as separate forms`() {
        val paths = listOf(
            "/admin/users/add" to "Add user",
            "/admin/users/$userId/change" to "Change user",
            "/admin/categories/add" to "Add category",
            "/admin/categories/$categoryId/change" to "Change category",
            "/admin/items/add" to "Add item",
            "/admin/items/$itemId/change" to "Change item",
        )

        paths.forEach { (path, title) ->
            val response = authenticatedGet(path)

            assertEquals(HttpStatus.OK.value(), response.statusCode(), path)
            assertTrue(response.body().contains(title), path)
            assertTrue(response.body().contains("Save"), path)
        }
    }

    private fun authenticatedGet(path: String): HttpResponse<String> {
        val credentials = Base64.getEncoder().encodeToString("admin:admin".toByteArray())
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port$path"))
            .header("Authorization", "Basic $credentials")
            .GET()
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }
}
