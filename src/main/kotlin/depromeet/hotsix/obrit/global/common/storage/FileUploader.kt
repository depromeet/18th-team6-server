package depromeet.hotsix.obrit.global.common.storage

import org.springframework.web.multipart.MultipartFile

interface FileUploader {
    fun upload(prefix: String, file: MultipartFile): String
}
