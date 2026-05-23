package depromeet.hotsix.obrit

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
class SwaggerDocumentationIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `exposes generated OpenAPI documentation`() {
        mockMvc.get("/v3/api-docs")
            .andExpect {
                status { isOk() }
                jsonPath("$.info.title") { value("Obrit API") }
                jsonPath("$.paths['/categories']") { exists() }
                jsonPath("$.paths['/items']") { exists() }
                jsonPath("$.paths['/items/{itemId}']") { exists() }
                jsonPath("$.paths['/items/{itemId}/replacements']") { exists() }
                jsonPath("$.paths['/home/items']") { exists() }
                jsonPath("$.paths['/categories'].get.parameters[?(@.name == 'X-User-Id' && @.required == true)]") {
                    exists()
                }
                jsonPath("$.paths['/home/items'].get.parameters[?(@.name == 'order')]") {
                    exists()
                }
            }
    }

    @Test
    fun `exposes Swagger UI`() {
        mockMvc.get("/swagger-ui/index.html")
            .andExpect {
                status { isOk() }
            }
    }
}
