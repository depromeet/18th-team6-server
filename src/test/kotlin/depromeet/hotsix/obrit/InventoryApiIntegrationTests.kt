package depromeet.hotsix.obrit

import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
    statements = [
        "DELETE FROM item_replacement_histories",
        "DELETE FROM items",
        "DELETE FROM categories",
        "DELETE FROM users",
        "INSERT INTO users (id, name, created_at, updated_at) VALUES (1, '지훈', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        "INSERT INTO users (id, name, created_at, updated_at) VALUES (2, '민지', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        "INSERT INTO categories (id, user_id, name, image_url, default_replacement_interval_days, created_at, updated_at) VALUES (100, NULL, '면도기', '/images/default-category.png', 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        "INSERT INTO categories (id, user_id, name, image_url, default_replacement_interval_days, created_at, updated_at) VALUES (200, NULL, '제로콜라', '/images/default-category.png', 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
    ],
)
class InventoryApiIntegrationTests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `lists preset and custom categories, and creates a custom category`() {
        mockMvc.get("/categories") {
            header("X-User-Id", "1")
        }.andExpect {
            status { isOk() }
            jsonPath("$[*].name", contains("면도기", "제로콜라"))
        }

        mockMvc.post("/categories") {
            header("X-User-Id", "1")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "name": "렌즈 세척액",
                  "defaultReplacementIntervalDays": 90
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.name") { value("렌즈 세척액") }
            jsonPath("$.imageUrl") { value("/images/default-category.png") }
            jsonPath("$.defaultReplacementIntervalDays") { value(90) }
        }

        mockMvc.get("/categories") {
            header("X-User-Id", "1")
        }.andExpect {
            status { isOk() }
            jsonPath("$[*].name", contains("면도기", "제로콜라", "렌즈 세척액"))
        }
    }

    @Test
    fun `rejects preset category deletion and soft deletes custom category with its items`() {
        mockMvc.delete("/categories/100") {
            header("X-User-Id", "1")
        }.andExpect {
            status { isBadRequest() }
        }

        val categoryId = createCustomCategory("렌즈 세척액", 90)
        createItem(categoryId, "욕실 렌즈 세척액", 1, "2026-04-01", null)

        mockMvc.delete("/categories/$categoryId") {
            header("X-User-Id", "1")
        }.andExpect {
            status { isNoContent() }
        }

        mockMvc.get("/categories") {
            header("X-User-Id", "1")
        }.andExpect {
            status { isOk() }
            jsonPath("$[*].name", contains("면도기", "제로콜라"))
            jsonPath("$[*].name", not(contains("렌즈 세척액")))
        }

        mockMvc.get("/items") {
            header("X-User-Id", "1")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(0) }
        }
    }

    @Test
    fun `rejects item creation with another user's custom category`() {
        val otherUserCategoryId = createCustomCategory(
            userId = 2,
            name = "렌즈 세척액",
            defaultReplacementIntervalDays = 90,
        )

        mockMvc.post("/items") {
            header("X-User-Id", "1")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "categoryId": $otherUserCategoryId,
                  "name": "욕실 렌즈 세척액",
                  "count": 1,
                  "lastReplacedDate": "2026-04-01"
                }
            """.trimIndent()
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.message") { value("Category not found.") }
        }
    }

    @Test
    fun `creates sorts updates and records replacements`() {
        val officeItemId = createItem(200, "사무실 제로콜라", 12, "2026-04-20", null)
        val homeItemId = createItem(200, "집 제로콜라", 6, "2026-04-18", 10)

        mockMvc.get("/items") {
            header("X-User-Id", "1")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data[0].id") { value(officeItemId.toInt()) }
            jsonPath("$.data[0].replacementIntervalDays") { value(7) }
            jsonPath("$.data[0].nextReplacementDate") { value("2026-04-27") }
            jsonPath("$.data[1].id") { value(homeItemId.toInt()) }
            jsonPath("$.data[1].replacementIntervalDays") { value(10) }
            jsonPath("$.data[1].nextReplacementDate") { value("2026-04-28") }
        }

        mockMvc.patch("/items/$homeItemId") {
            header("X-User-Id", "1")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "replacementIntervalDays": 5
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.replacementIntervalDays") { value(5) }
            jsonPath("$.data.nextReplacementDate") { value("2026-04-23") }
        }

        mockMvc.post("/items/$officeItemId/replacements") {
            header("X-User-Id", "1")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "replacedDate": "2026-04-25"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.lastReplacedDate") { value("2026-04-25") }
            jsonPath("$.data.nextReplacementDate") { value("2026-05-02") }
        }
    }

    private fun createCustomCategory(name: String, defaultReplacementIntervalDays: Int): Long = createCustomCategory(
        userId = 1,
        name = name,
        defaultReplacementIntervalDays = defaultReplacementIntervalDays,
    )

    private fun createCustomCategory(userId: Long, name: String, defaultReplacementIntervalDays: Int): Long {
        val response = mockMvc.post("/categories") {
            header("X-User-Id", userId.toString())
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "name": "$name",
                  "defaultReplacementIntervalDays": $defaultReplacementIntervalDays
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
        }.andReturn().response.contentAsString

        return objectMapper.readTree(response).get("id").asLong()
    }

    private fun createItem(
        categoryId: Long,
        name: String,
        count: Int,
        lastReplacedDate: String,
        replacementIntervalDays: Int?,
    ): Long {
        val intervalField = replacementIntervalDays?.let { """, "replacementIntervalDays": $it""" }.orEmpty()
        val response = mockMvc.post("/items") {
            header("X-User-Id", "1")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "categoryId": $categoryId,
                  "name": "$name",
                  "count": $count,
                  "lastReplacedDate": "$lastReplacedDate"
                  $intervalField
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
        }.andReturn().response.contentAsString

        return objectMapper.readTree(response).get("data").get("id").asLong()
    }
}
