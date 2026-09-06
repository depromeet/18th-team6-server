package depromeet.hotsix.obrit.notification

import depromeet.hotsix.obrit.notification.config.firebase.FirebaseConfig
import depromeet.hotsix.obrit.notification.config.firebase.FirebaseProperties
import depromeet.hotsix.obrit.notification.entity.FirebaseInitializationState
import depromeet.hotsix.obrit.notification.service.FirebaseStatusService
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * 자격증명이 없거나 잘못돼도 기동이 막히지 않아야 한다.
 *
 * [FirebaseConfig]는 `@Profile("!test")`라 테스트 컨텍스트에 올라오지 않으므로, 초기화 메서드를
 * 직접 호출해 예외가 밖으로 나가지 않는지 확인한다. 예외가 나가면 실제 기동에서는
 * BeanCreationException이 되어 컨텍스트 전체가 죽는다.
 */
class FirebaseInitializationTest {

    private fun initializeWith(credentialsPath: String): FirebaseStatusService {
        val status = FirebaseStatusService()
        FirebaseConfig(FirebaseProperties(credentialsPath), status).initialize()
        return status
    }

    @Test
    fun `자격증명 파일이 없어도 예외를 던지지 않는다`() {
        val status = initializeWith("/no/such/firebase-credentials.json")

        assertEquals(FirebaseInitializationState.FAILED, status.state)
    }

    @Test
    fun `환경변수가 치환되지 않은 경로도 기동을 막지 않는다`() {
        // 2026-09-04 운영 장애와 같은 형태. placeholder가 문자열 그대로 경로가 됐다.
        val status = initializeWith("\${FIREBASE_CREDENTIALS_PATH}")

        assertEquals(FirebaseInitializationState.FAILED, status.state)
    }

    @Test
    fun `초기화에 실패하면 실패 원인이 남는다`() {
        val status = initializeWith("/no/such/firebase-credentials.json")

        assertNotNull(status.failureReason)
    }

    @Test
    fun `초기화에 실패하면 발송을 시도하지 않는다`() {
        val status = initializeWith("/no/such/firebase-credentials.json")

        assertFalse(status.canSend)
    }

    @Test
    fun `초기화를 시도하지 않은 상태에서도 발송하지 않는다`() {
        val status = FirebaseStatusService()

        assertEquals(FirebaseInitializationState.NOT_ATTEMPTED, status.state)
        assertFalse(status.canSend)
    }
}
