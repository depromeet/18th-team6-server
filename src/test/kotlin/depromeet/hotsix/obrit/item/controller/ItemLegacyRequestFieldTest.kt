package depromeet.hotsix.obrit.item.controller

import depromeet.hotsix.obrit.category.entity.Category
import depromeet.hotsix.obrit.category.entity.CategoryIcon
import depromeet.hotsix.obrit.category.repository.CategoryIconFixture
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Clock
import java.time.LocalDate
import kotlin.test.assertEquals

/**
 * v2.1에서 삭제한 요청 필드를 구버전 앱이 계속 보내도 요청이 실패하지 않고 값은 무시되는지 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ItemLegacyRequestFieldTest {

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
    private lateinit var clock: Clock

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private var userId: Long = 0L
    private var categoryId: Long = 0L

    private val today: LocalDate
        get() = LocalDate.now(clock)

    @BeforeEach
    fun setUp() {
        itemRepository.deleteAll()
        categoryRepository.deleteAll()
        categoryIconRepository.deleteAll()
        userRepository.deleteAll()

        CategoryIconFixture.ensureDefaultIcon(jdbcTemplate)
        val user = userRepository.save(User(uuid = "6f1c2a7e-3b9d-4e8a-9c1f-2d4b6a8e0c13"))
        val icon = categoryIconRepository.save(CategoryIcon(name = "legacy", key = "", url = "/icons/legacy.png"))
        val category = categoryRepository.save(
            Category(userId = null, name = "칫솔", iconId = icon.id, defaultReplacementIntervalDays = 30),
        )

        userId = requireNotNull(user.id)
        categoryId = requireNotNull(category.id)
    }

    @Test
    fun `등록 요청에 마지막 교체 시기와 새 소모품 주기가 와도 무시하고 오늘로 등록한다`() {
        val response = send(
            "POST",
            "/items",
            """
            {
              "newCategoryName": "수세미",
              "newCategoryDefaultReplacementIntervalDays": 1,
              "name": "주방 수세미",
              "lastReplacementPeriod": "OVER_THREE_MONTHS"
            }
            """.trimIndent(),
        )

        assertEquals(HttpStatus.CREATED.value(), response.statusCode())
        val item = itemRepository.findActiveByUserId(userId).single()
        assertEquals(today, item.lastReplacedDate)
        assertEquals(Category.USER_DEFAULT_REPLACEMENT_INTERVAL_DAYS, item.replacementIntervalDays)
    }

    @Test
    fun `수정 요청에 마지막 교체일이 와도 바꾸지 않는다`() {
        val lastReplacedDate = today.minusDays(10)
        val item = itemRepository.save(
            Item(
                userId = userId,
                categoryId = categoryId,
                name = "회사용 칫솔",
                replacementIntervalDays = 30,
                lastReplacedDate = lastReplacedDate,
                nextReplacementDate = lastReplacedDate.plusDays(30),
            ),
        )

        val response = send(
            "PATCH",
            "/items/${item.id}",
            """{ "name": "집 칫솔", "lastReplacedDate": "${today.minusDays(100)}" }""",
        )

        assertEquals(HttpStatus.OK.value(), response.statusCode())
        val updated = itemRepository.findById(requireNotNull(item.id)).orElseThrow()
        assertEquals("집 칫솔", updated.name)
        assertEquals(lastReplacedDate, updated.lastReplacedDate)
        assertEquals(lastReplacedDate.plusDays(30), updated.nextReplacementDate)
    }

    private fun send(method: String, path: String, body: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:$port$path"))
            .header("Content-Type", "application/json")
            .header("X-User-Id", userId.toString())
            .method(method, HttpRequest.BodyPublishers.ofString(body))
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }
}
