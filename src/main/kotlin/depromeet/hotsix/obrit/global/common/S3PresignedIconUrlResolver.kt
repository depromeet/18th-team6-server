package depromeet.hotsix.obrit.global.common

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration

@Component
@Profile("prod")
class S3PresignedIconUrlResolver(private val s3Presigner: S3Presigner, private val s3Properties: S3Properties) :
    IconUrlResolver {

    override fun resolve(key: String): String {
        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofHours(1))
            .getObjectRequest { it.bucket(s3Properties.bucket).key(key) }
            .build()

        return s3Presigner.presignGetObject(presignRequest).url().toString()
    }
}
