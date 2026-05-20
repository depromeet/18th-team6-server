package depromeet.hotsix.obrit.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "firebase")
data class FirebaseProperties(val credentialsPath: String)
