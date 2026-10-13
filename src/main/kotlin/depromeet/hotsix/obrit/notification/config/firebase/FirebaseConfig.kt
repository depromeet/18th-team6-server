package depromeet.hotsix.obrit.notification.config.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import depromeet.hotsix.obrit.notification.service.FirebaseStatusService
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.io.FileInputStream

/**
 * Firebase 초기화. **실패해도 애플리케이션 기동을 막지 않는다.**
 *
 * 예외를 그대로 흘리면 BeanCreationException으로 컨텍스트 전체가 죽어, 알림 설정 하나 때문에
 * 소모품 조회·등록·교체까지 함께 내려간다. 실제로 2026-09-04 운영 기동 실패가 이렇게 발생했다.
 *
 * 대신 실패를 [FirebaseStatusService]에 남긴다. 조용히 안 나가는 상태가 더 위험하므로
 * ERROR 로그와 관리 화면 양쪽에서 드러나야 한다.
 */
@Configuration
@Profile("!test")
@EnableConfigurationProperties(FirebaseProperties::class)
class FirebaseConfig(
    private val firebaseProperties: FirebaseProperties,
    private val firebaseStatusService: FirebaseStatusService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                val credentials = FileInputStream(firebaseProperties.credentialsPath).use {
                    GoogleCredentials.fromStream(it)
                }
                FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(credentials).build())
            }
            firebaseStatusService.markInitialized()
        } catch (e: Exception) {
            // 자격증명 파일은 서버에 수동 배치되어 CI로 전달되지 않는다. 서버를 재구축하면 다시 사라진다.
            log.error("Firebase 초기화 실패. 알림 발송이 비활성화된 채로 기동한다. path={}", firebaseProperties.credentialsPath, e)
            firebaseStatusService.markFailed(e.message ?: e.javaClass.simpleName)
        }
    }
}
