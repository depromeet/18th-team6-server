package depromeet.hotsix.obrit.global.common.storage

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component("s3PublicUrlResolver")
@Profile("!prod")
class PassthroughUrlResolver : UrlResolver {
    override fun resolve(key: String): String = key
}
