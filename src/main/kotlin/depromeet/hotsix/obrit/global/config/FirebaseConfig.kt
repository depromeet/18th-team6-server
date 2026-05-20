package depromeet.hotsix.obrit.global.config

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
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(FileInputStream(firebaseProperties.credentialsPath)))
                .build()
            FirebaseApp.initializeApp(options)
        }
    }
}
