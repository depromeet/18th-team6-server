package depromeet.hotsix.obrit.global.common.storage

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.UUID

@Component
@Profile("prod")
class S3Uploader(private val s3Client: S3Client, private val s3Properties: S3Properties) : FileUploader {

    override fun upload(prefix: String, file: MultipartFile): String {
        val extension = file.originalFilename?.substringAfterLast('.', "") ?: ""
        val filename = UUID.randomUUID().toString()
        val key = if (extension.isNotBlank()) "$prefix/$filename.$extension" else "$prefix/$filename"

        val request = PutObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(key)
            .contentType(file.contentType)
            .build()

        s3Client.putObject(request, RequestBody.fromInputStream(file.inputStream, file.size))
        return key
    }
}
