package depromeet.hotsix.obrit.admin.controller

import depromeet.hotsix.obrit.category.entity.Category
import depromeet.hotsix.obrit.category.entity.CategoryIcon
import depromeet.hotsix.obrit.category.repository.CategoryIconRepository
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.global.log.access.entity.ApiAccessLog
import depromeet.hotsix.obrit.global.log.access.repository.ApiAccessLogRepository
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

    @Autowired
    private lateinit var apiAccessLogRepository: ApiAccessLogRepository

    private var userId: Long = 0L
    private lateinit var userCreatedAt: LocalDateTime
    private var iconId: Long = 0L
    private var categoryId: Long = 0L
    private var itemId: Long = 0L

    @BeforeEach
    fun setUp() {
        apiAccessLogRepository.deleteAll()
        itemRepository.deleteAll()
        categoryRepository.deleteAll()
        categoryIconRepository.deleteAll()
        userRepository.deleteAll()

        val user = userRepository.saveAndFlush(
            User(uuid = "admin-controller-user", name = "Admin Controller User"),
        )
        userId = requireNotNull(user.id)
        userCreatedAt = requireNotNull(user.createdAt)

        val icon = categoryIconRepository.save(CategoryIcon(name = "default", key = "", url = "/icons/default.png"))
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
        apiAccessLogRepository.saveAll(
            listOf(
                ApiAccessLog(
                    userId = userId,
                    method = "GET",
                    path = "/categories",
                    pathTemplate = "/categories",
                    statusCode = 200,
                    durationMs = 8,
                    occurredAt = userCreatedAt.plusSeconds(1),
                ),
                ApiAccessLog(
                    userId = userId,
                    method = "POST",
                    path = "/items",
                    pathTemplate = "/items",
                    statusCode = 201,
                    durationMs = 20,
                    occurredAt = userCreatedAt.plusSeconds(8),
                ),
                ApiAccessLog(
                    userId = userId,
                    method = "GET",
                    path = "/home/items",
                    pathTemplate = "/home/items",
                    statusCode = 200,
                    durationMs = 12,
                    occurredAt = userCreatedAt.plusSeconds(12),
                ),
                ApiAccessLog(
                    userId = userId,
                    method = "PATCH",
                    path = "/items/1/spare-count",
                    pathTemplate = "/items/{itemId}/spare-count",
                    statusCode = 200,
                    durationMs = 18,
                    occurredAt = userCreatedAt.plusSeconds(20),
                ),
                ApiAccessLog(
                    userId = userId,
                    method = "POST",
                    path = "/receipts/analyze",
                    pathTemplate = "/receipts/analyze",
                    statusCode = 200,
                    durationMs = 120,
                    occurredAt = userCreatedAt.plusSeconds(25),
                ),
            ),
        )

        iconId = icon.id
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
                response.statusCode() == HttpStatus.FORBIDDEN.value() ||
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
        val icons = authenticatedGet("/admin/icons")
        val categories = authenticatedGet("/admin/categories")
        val items = authenticatedGet("/admin/items")

        assertEquals(HttpStatus.OK.value(), users.statusCode())
        assertEquals(HttpStatus.OK.value(), icons.statusCode())
        assertEquals(HttpStatus.OK.value(), categories.statusCode())
        assertEquals(HttpStatus.OK.value(), items.statusCode())

        assertTrue(users.body().contains("Add user"))
        assertTrue(users.body().contains("/admin/users/$userId/change"))
        assertFalse(users.body().contains("Create user"))

        assertTrue(icons.body().contains("Add icon"))
        assertTrue(icons.body().contains("/admin/icons/$iconId/change"))
        assertTrue(icons.body().contains("""src="/icons/default.png""""))

        assertTrue(categories.body().contains("Add category"))
        assertTrue(categories.body().contains("/admin/categories/$categoryId/change"))
        assertTrue(categories.body().contains("""src="/icons/default.png""""))
        assertTrue(categories.body().contains("""alt="Controller Category icon""""))

        assertTrue(items.body().contains("Add item"))
        assertTrue(items.body().contains("/admin/items/$itemId/change"))
        assertTrue(items.body().contains("/admin/analytics/signup-funnel"))
    }

    @Test
    fun `admin add and change pages render as separate forms`() {
        val paths = listOf(
            "/admin/users/add" to "Add user",
            "/admin/users/$userId/change" to "Change user",
            "/admin/icons/add" to "Add icon",
            "/admin/icons/$iconId/change" to "Change icon",
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
            assertTrue(response.body().contains("""name="_csrf""""))
        }
    }

    @Test
    fun `admin category forms render icon previews`() {
        val add = authenticatedGet("/admin/categories/add")
        val change = authenticatedGet("/admin/categories/$categoryId/change")

        assertEquals(HttpStatus.OK.value(), add.statusCode())
        assertEquals(HttpStatus.OK.value(), change.statusCode())
        assertTrue(add.body().contains("""src="/icons/default.png""""))
        assertTrue(change.body().contains("""src="/icons/default.png""""))
    }

    @Test
    fun `admin signup funnel page renders journey timeline`() {
        val startAt = userCreatedAt.minusMinutes(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val endAt = userCreatedAt.plusMinutes(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val response = authenticatedGet("/admin/analytics/signup-funnel?startAt=$startAt&endAt=$endAt&userId=$userId")

        assertEquals(HttpStatus.OK.value(), response.statusCode())
        assertTrue(response.body().contains("Signup Funnel Journey"))
        assertTrue(response.body().contains("1_signup"))
        assertTrue(response.body().contains("4_additional_action"))
        assertTrue(response.body().contains("5_ocr_used"))
        assertTrue(response.body().contains("/items/{itemId}/spare-count"))
        assertTrue(response.body().contains("additional_action"))
        assertTrue(response.body().contains("ocr_used"))
    }

    @Test
    fun `admin user journey page renders core actions`() {
        val startAt = userCreatedAt.minusMinutes(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val endAt = userCreatedAt.plusMinutes(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val response = authenticatedGet("/admin/analytics/user-journey?startAt=$startAt&endAt=$endAt")

        assertEquals(HttpStatus.OK.value(), response.statusCode())
        assertTrue(response.body().contains("User Journey"))
        assertTrue(response.body().contains("Core Action Reach"))
        assertTrue(response.body().contains("소모품 등록"))
        assertTrue(response.body().contains("여분 수정"))
        assertTrue(response.body().contains("Receipt Analysis"))
        assertTrue(response.body().contains("30-day Revisit"))
        // setUp의 접근 로그가 등록·여분수정·영수증을 모두 남기므로 1명 도달로 잡힌다
        assertTrue(response.body().contains("1 active users"))
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
