package depromeet.hotsix.obrit

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class SeedDataIntegrationTests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `development seed user can create a custom category`() {
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
        }
    }
}
