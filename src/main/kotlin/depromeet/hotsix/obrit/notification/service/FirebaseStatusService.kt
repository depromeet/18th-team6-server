package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.notification.entity.FirebaseInitializationState
import org.springframework.stereotype.Component

/**
 * Firebase 초기화 결과를 담아 발송 경로와 관리 화면이 함께 본다.
 *
 * 초기화는 기동 스레드에서, 조회는 배치·요청 스레드에서 일어나므로 값을 volatile로 둔다.
 */
@Component
class FirebaseStatusService {

    @Volatile
    private var currentState: FirebaseInitializationState = FirebaseInitializationState.NOT_ATTEMPTED

    /** 실패 원인. 관리 화면에 그대로 노출하므로 예외 메시지만 담고 스택 트레이스는 로그에 남긴다. */
    @Volatile
    private var currentFailureReason: String? = null

    val state: FirebaseInitializationState
        get() = currentState

    val failureReason: String?
        get() = currentFailureReason

    val canSend: Boolean
        get() = currentState == FirebaseInitializationState.INITIALIZED

    fun markInitialized() {
        currentState = FirebaseInitializationState.INITIALIZED
        currentFailureReason = null
    }

    fun markFailed(reason: String) {
        currentState = FirebaseInitializationState.FAILED
        currentFailureReason = reason
    }
}
