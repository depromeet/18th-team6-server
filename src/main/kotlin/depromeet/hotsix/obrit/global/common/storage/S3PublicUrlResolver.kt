package depromeet.hotsix.obrit.global.common.storage

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component("s3PublicUrlResolver")
@Profile("prod")
class S3PublicUrlResolver(private val s3Properties: S3Properties) : UrlResolver {

    override fun resolve(key: String): String =
        "https://${s3Properties.bucket}.s3.${s3Properties.region}.amazonaws.com/$key"
}
