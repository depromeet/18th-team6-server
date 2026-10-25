package depromeet.hotsix.obrit.global.common.storage

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Files
import java.nio.file.Path

class FileSystemFileUploaderTest {

    @TempDir
    lateinit var rootDir: Path

    private val uploader by lazy {
        FileSystemFileUploader(StorageProperties(rootDir = rootDir, publicBaseUrl = "https://api.example.com"))
    }

    @Test
    fun `업로드한 파일은 prefix 아래에 확장자를 유지해 저장하고 키를 반환한다`() {
        val content = byteArrayOf(1, 2, 3)

        val key = uploader.upload("category-icon", MockMultipartFile("file", "icon.webp", "image/webp", content))

        assertTrue(Regex("category-icon/[0-9a-f-]{36}\\.webp").matches(key), key)
        assertArrayEquals(content, Files.readAllBytes(rootDir.resolve(key)))
    }

    // 원본 파일명에 경로 조작 문자가 있어도 저장 디렉터리 밖에 쓰지 않는다.
    @Test
    fun `영숫자가 아닌 확장자는 버리고 prefix 디렉터리 안에 저장한다`() {
        val key = uploader.upload("receipts", MockMultipartFile("file", "a./../../evil", "image/png", byteArrayOf(1)))

        assertTrue(Regex("receipts/[0-9a-f-]{36}").matches(key), key)
        assertTrue(Files.exists(rootDir.resolve(key)))
        val topLevelNames = Files.list(rootDir).use { files -> files.map { it.fileName.toString() }.toList() }
        assertEquals(listOf("receipts"), topLevelNames)
    }

    @Test
    fun `공개 URL은 외부 주소 뒤에 files 경로와 키를 붙인다`() {
        val resolver = FileSystemPublicUrlResolver(
            StorageProperties(rootDir = rootDir, publicBaseUrl = "https://api.example.com/"),
        )

        assertEquals(
            "https://api.example.com/files/category-icon/bandage.webp",
            resolver.resolve("category-icon/bandage.webp"),
        )
    }
}
