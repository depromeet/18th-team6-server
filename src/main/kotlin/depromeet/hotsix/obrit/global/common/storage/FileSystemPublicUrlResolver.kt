package depromeet.hotsix.obrit.global.common.storage

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

// 기존 주입 지점(@Qualifier("s3PublicUrlResolver"))을 그대로 쓰도록 같은 빈 이름으로 등록한다.
@Component("s3PublicUrlResolver")
@Profile("prod")
@ConditionalOnProperty(prefix = "storage", name = ["type"], havingValue = "filesystem", matchIfMissing = true)
class FileSystemPublicUrlResolver(private val storageProperties: StorageProperties) : UrlResolver {

    override fun resolve(key: String): String =
        "${storageProperties.publicBaseUrl.trimEnd('/')}${StorageProperties.PUBLIC_PATH}/$key"
}
