package depromeet.hotsix.obrit.global.config

import depromeet.hotsix.obrit.global.common.storage.S3Properties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
@Profile("prod")
@EnableConfigurationProperties(S3Properties::class)
class S3Config(private val s3Properties: S3Properties) {

    @Bean
    fun s3Presigner(): S3Presigner = S3Presigner.builder()
        .region(Region.of(s3Properties.region))
        .build()
}
