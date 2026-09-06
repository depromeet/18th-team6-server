package depromeet.hotsix.obrit.global.common.storage

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

@Component
@Profile("!prod & !test")
class LocalFileUploader(private val baseDir: Path = Path.of("build", "local-uploads")) : FileUploader {

    override fun upload(prefix: String, file: MultipartFile): String {
        val extension = file.originalFilename?.substringAfterLast('.', "") ?: ""
        val filename = UUID.randomUUID().toString()
        val key = if (extension.isNotBlank()) "$prefix/$filename.$extension" else "$prefix/$filename"

        val target = baseDir.resolve(key)
        target.parent.createDirectories()
        target.writeBytes(file.bytes)
        return key
    }

    override fun download(key: String): ByteArray {
        val target = baseDir.resolve(key)
        require(Files.exists(target)) { "로컬 스토리지에 파일이 없습니다: $key" }
        return target.readBytes()
    }
}
