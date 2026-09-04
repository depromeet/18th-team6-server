package depromeet.hotsix.obrit.notification.config.firebase

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "firebase")
data class FirebaseProperties(val credentialsPath: String)
