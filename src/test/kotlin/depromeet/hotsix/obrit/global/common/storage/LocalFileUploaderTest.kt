package depromeet.hotsix.obrit.global.common.storage

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Path
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class LocalFileUploaderTest {

    @Test
    fun `upload한 key로 download하면 저장한 파일을 읽어온다`(@TempDir tempDir: Path) {
        val uploader = LocalFileUploader(tempDir)
        val bytes = "hello-receipt".toByteArray()
        val file = MockMultipartFile("image", "receipt.jpg", "image/jpeg", bytes)

        val key = uploader.upload("receipts", file)
        val downloaded = uploader.download(key)

        assertContentEquals(bytes, downloaded)
    }

    @Test
    fun `존재하지_않는_key를_download하면_예외를_던진다`(@TempDir tempDir: Path) {
        val uploader = LocalFileUploader(tempDir)

        assertFailsWith<Exception> {
            uploader.download("receipts/does-not-exist.jpg")
        }
    }
}
