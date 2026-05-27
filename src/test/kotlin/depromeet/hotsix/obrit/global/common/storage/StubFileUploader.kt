package depromeet.hotsix.obrit.global.common.storage

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
@Profile("test")
class StubFileUploader : FileUploader {

    override fun upload(prefix: String, file: MultipartFile): String = "$prefix/${file.originalFilename}"
}
