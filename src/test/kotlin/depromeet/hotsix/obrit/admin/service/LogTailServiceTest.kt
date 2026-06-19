package depromeet.hotsix.obrit.admin.service

import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogTailServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private fun service(): LogTailService = LogTailService(tempDir)

    private fun writeLog(name: String, content: String) {
        Files.writeString(tempDir.resolve(name), content)
    }

    private fun writeGzLog(name: String, content: String) {
        val gzBytes = ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { it.write(content.toByteArray()) }
            baos.toByteArray()
        }
        Files.write(tempDir.resolve(name), gzBytes)
    }

    @Test
    fun `listFiles - 디렉토리가 없으면 빈 리스트를 반환한다`() {
        val emptyDir = tempDir.resolve("non-existent")
        val result = LogTailService(emptyDir).listFiles()
        assertEquals(emptyList(), result)
    }

    @Test
    fun `tail - 화이트리스트 불일치 파일명은 400`() {
        assertThrows<BusinessException> { service().tail("../etc/passwd", 100) }
        assertThrows<BusinessException> { service().tail("random.txt", 100) }
    }

    @Test
    fun `tail - canonical path가 logs 바깥이면 400`() {
        val outside = Files.createTempFile("outside", ".log")
        Files.writeString(outside, "secret\n")
        val link = tempDir.resolve("obrit.log")
        Files.createSymbolicLink(link, outside)

        assertThrows<BusinessException> { service().tail("obrit.log", 100) }

        Files.deleteIfExists(link)
        Files.deleteIfExists(outside)
    }

    @Test
    fun `tail - lines가 범위 밖이면 400`() {
        writeLog("obrit.log", "line1\n")
        assertThrows<BusinessException> { service().tail("obrit.log", 0) }
        assertThrows<BusinessException> { service().tail("obrit.log", -1) }
        assertThrows<BusinessException> { service().tail("obrit.log", 1001) }
    }

    @Test
    fun `tail - 존재하지 않는 파일은 404`() {
        assertThrows<ResourceNotFoundException> { service().tail("obrit.log", 100) }
    }

    @Test
    fun `tail - 일반 파일의 마지막 N줄을 반환한다`() {
        writeLog("obrit.log", (1..10).joinToString("\n") { "line$it" } + "\n")
        val result = service().tail("obrit.log", 3)
        assertEquals("line8\nline9\nline10\n", result)
    }

    @Test
    fun `tail - gz 파일도 마지막 N줄을 반환한다`() {
        writeGzLog("obrit.2026-06-18.0.log.gz", (1..5).joinToString("\n") { "g$it" } + "\n")
        val result = service().tail("obrit.2026-06-18.0.log.gz", 2)
        assertEquals("g4\ng5\n", result)
    }

    @Test
    fun `listFiles - 화이트리스트 매칭 파일만 lastModified 내림차순으로 반환`() {
        writeLog("obrit.log", "active\n")
        Thread.sleep(10)
        writeLog("obrit.2026-06-18.0.log.gz", "old\n")
        writeLog("ignore.txt", "no")

        val result = service().listFiles()

        assertEquals(2, result.size)
        assertTrue(result[0].lastModified >= result[1].lastModified)
        assertTrue(result.map { it.name }.containsAll(listOf("obrit.log", "obrit.2026-06-18.0.log.gz")))
    }
}
