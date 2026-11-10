package depromeet.hotsix.obrit.global.config

import depromeet.hotsix.obrit.global.exception.GlobalExceptionHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import java.nio.file.Files
import java.nio.file.Path

// 전체 앱 없이 StorageConfig의 정적 리소스 노출 범위만 검증한다.
@SpringJUnitWebConfig(
    classes = [StorageConfig::class, GlobalExceptionHandler::class, StorageConfigTest.WebConfig::class],
)
@ActiveProfiles("prod")
class StorageConfigTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    @Test
    fun `아이콘 파일은 files 경로로 캐시 헤더와 함께 내려준다`() {
        mockMvc.get("/files/category-icon/bandage.webp").andExpect {
            status { isOk() }
            content { bytes(ICON_BYTES) }
            header { string("Cache-Control", "max-age=2592000, public") }
        }
    }

    @Test
    fun `없는 아이콘 파일은 404를 반환한다`() {
        mockMvc.get("/files/category-icon/missing.webp").andExpect { status { isNotFound() } }
    }

    @Test
    fun `영수증 파일은 저장되어 있어도 내려주지 않는다`() {
        mockMvc.get("/files/receipts/receipt.jpg").andExpect { status { isNotFound() } }
    }

    @Test
    fun `상위 경로로 벗어나는 요청은 내려주지 않는다`() {
        mockMvc.get("/files/category-icon/../receipts/receipt.jpg").andExpect { status { isNotFound() } }
    }

    @Configuration
    @EnableWebMvc
    class WebConfig

    companion object {
        private val ICON_BYTES = byteArrayOf(1, 2, 3)
        private val rootDir: Path = Files.createTempDirectory("storage-config-test").also {
            Files.createDirectories(it.resolve("category-icon"))
            Files.write(it.resolve("category-icon/bandage.webp"), ICON_BYTES)
            Files.createDirectories(it.resolve("receipts"))
            Files.write(it.resolve("receipts/receipt.jpg"), byteArrayOf(9))
        }

        @JvmStatic
        @DynamicPropertySource
        fun storageProperties(registry: DynamicPropertyRegistry) {
            registry.add("storage.root-dir") { rootDir.toString() }
            registry.add("storage.public-base-url") { "https://api.example.com" }
        }
    }
}
