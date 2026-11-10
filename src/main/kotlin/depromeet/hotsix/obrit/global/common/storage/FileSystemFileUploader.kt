package depromeet.hotsix.obrit.global.common.storage

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.util.UUID

@Component
@Profile("prod")
@ConditionalOnProperty(prefix = "storage", name = ["type"], havingValue = "filesystem", matchIfMissing = true)
class FileSystemFileUploader(private val storageProperties: StorageProperties) : FileUploader {

    override fun upload(prefix: String, file: MultipartFile): String {
        // 확장자는 영숫자만 허용해 경로 조작(../)으로 저장 디렉터리를 벗어나지 못하게 한다.
        val extension = file.originalFilename?.substringAfterLast('.', "")
            ?.takeIf { SAFE_EXTENSION.matches(it) }
            .orEmpty()
        val filename = UUID.randomUUID().toString()
        val key = if (extension.isNotBlank()) "$prefix/$filename.$extension" else "$prefix/$filename"

        val target = storageProperties.rootDir.resolve(key)
        Files.createDirectories(target.parent)
        file.inputStream.use { Files.copy(it, target) }
        return key
    }

    override fun download(key: String): ByteArray {
        val root = storageProperties.rootDir.normalize()
        val target = root.resolve(key).normalize()
        require(target.startsWith(root)) { "잘못된 파일 키입니다: $key" }
        require(Files.exists(target)) { "파일 저장소에 파일이 없습니다: $key" }
        return Files.readAllBytes(target)
    }

    companion object {
        private val SAFE_EXTENSION = Regex("[A-Za-z0-9]{1,10}")
    }
}
