package depromeet.hotsix.obrit.notification.config.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.io.FileInputStream

@Configuration
@Profile("!test")
@EnableConfigurationProperties(FirebaseProperties::class)
class FirebaseConfig(private val firebaseProperties: FirebaseProperties) {
    @PostConstruct
    fun initialize() {
        if (FirebaseApp.getApps().isEmpty()) {
            val credentials = FileInputStream(firebaseProperties.credentialsPath).use {
                GoogleCredentials.fromStream(it)
            }
            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build()
            FirebaseApp.initializeApp(options)
        }
    }
}
