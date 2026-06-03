package depromeet.hotsix.obrit.item.controller

import depromeet.hotsix.obrit.category.entity.Category
import depromeet.hotsix.obrit.category.entity.CategoryIcon
import depromeet.hotsix.obrit.category.repository.CategoryIconRepository
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.item.dto.CreateItemRequest
import depromeet.hotsix.obrit.item.entity.LastReplacementPeriod
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
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ItemControllerBulkTest {

    @LocalServerPort
    private var port: Int = 0

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var categoryIconRepository: CategoryIconRepository

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private var userId: Long = 0L
    private var categoryId: Long = 0L

    @BeforeEach
    fun setUp() {
        itemRepository.deleteAll()
        categoryRepository.deleteAll()
        categoryIconRepository.deleteAll()
        userRepository.deleteAll()

        val user = userRepository.save(User(uuid = "550e8400-e29b-41d4-a716-446655440000"))
        val icon = categoryIconRepository.save(CategoryIcon(name = "bulk", key = "", url = "/icons/bulk.png"))
        val category = categoryRepository.save(
            Category(
                userId = null,
                name = "Bulk Category",
                iconId = icon.id,
                defaultReplacementIntervalDays = 30,
            ),
        )

        userId = requireNotNull(user.id)
        categoryId = requireNotNull(category.id)
    }

    @Test
    fun `bulk create accepts up to 20 items`() {
        val response = postBulkCreate(itemCount = 20)

        assertEquals(HttpStatus.CREATED.value(), response.statusCode())
        assertTrue(response.body().contains(""""success":true"""))
        assertEquals(20, itemRepository.findActiveByUserId(userId).size)
    }

    @Test
    fun `bulk create rejects more than 20 items`() {
        val response = postBulkCreate(itemCount = 21)

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode())
        assertEquals("""{"message":"소모품 목록은 1개 이상 20개 이하여야 합니다."}""", response.body())
        assertEquals(0, itemRepository.findActiveByUserId(userId).size)
    }

    private fun postBulkCreate(itemCount: Int): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port/items/bulk"))
            .header("Content-Type", "application/json")
            .header("X-User-Id", userId.toString())
            .POST(HttpRequest.BodyPublishers.ofString(bulkRequestBody(itemCount)))
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun bulkRequestBody(itemCount: Int): String {
        val items = (1..itemCount).map { index ->
            CreateItemRequest(
                categoryId = categoryId,
                name = "Bulk Item $index",
                spareQuantity = index,
                lastReplacementPeriod = LastReplacementPeriod.WITHIN_WEEK,
            )
        }
        return objectMapper.writeValueAsString(mapOf("items" to items))
    }
}
