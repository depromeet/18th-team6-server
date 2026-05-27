package depromeet.hotsix.obrit.global.common

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cloud.aws.s3")
data class S3Properties(val bucket: String, val region: String)
