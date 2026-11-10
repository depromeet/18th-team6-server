package depromeet.hotsix.obrit.global.config

import depromeet.hotsix.obrit.global.common.storage.StorageProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Duration

@Configuration
@Profile("prod")
@ConditionalOnProperty(prefix = "storage", name = ["type"], havingValue = "filesystem", matchIfMissing = true)
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfig(private val storageProperties: StorageProperties) : WebMvcConfigurer {

    // 공개 prefix만 정적 리소스로 노출한다. 업로드 키는 매번 새 UUID라 내용이 바뀌지 않아 길게 캐시한다.
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val prefix = StorageProperties.PUBLIC_ICON_PREFIX
        val location = storageProperties.rootDir.resolve(prefix).toUri().toString().trimEnd('/') + "/"
        registry.addResourceHandler("${StorageProperties.PUBLIC_PATH}/$prefix/**")
            .addResourceLocations(location)
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(ICON_CACHE_DAYS)).cachePublic())
    }

    companion object {
        private const val ICON_CACHE_DAYS = 30L
    }
}
