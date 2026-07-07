package depromeet.hotsix.obrit.global.common.storage

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.ConcurrentHashMap

@Component
@Profile("test")
class StubFileUploader : FileUploader {

    private val store = ConcurrentHashMap<String, ByteArray>()

    override fun upload(prefix: String, file: MultipartFile): String {
        val key = "$prefix/${file.originalFilename}"
        store[key] = file.bytes
        return key
    }

    override fun download(key: String): ByteArray =
        store[key] ?: throw NoSuchElementException("스텁 스토리지에 파일이 없습니다: $key")
}
