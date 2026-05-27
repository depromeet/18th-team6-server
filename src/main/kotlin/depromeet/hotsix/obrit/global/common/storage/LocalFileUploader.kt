package depromeet.hotsix.obrit.global.common.storage

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Component
@Profile("!prod & !test")
class LocalFileUploader : FileUploader {

    override fun upload(prefix: String, file: MultipartFile): String {
        val extension = file.originalFilename?.substringAfterLast('.', "") ?: ""
        val filename = UUID.randomUUID().toString()
        return if (extension.isNotBlank()) "$prefix/$filename.$extension" else "$prefix/$filename"
    }
}
