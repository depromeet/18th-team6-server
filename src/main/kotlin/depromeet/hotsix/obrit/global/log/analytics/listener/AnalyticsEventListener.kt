package depromeet.hotsix.obrit.global.log.analytics.listener

import depromeet.hotsix.obrit.global.log.analytics.event.SignupCompletedDomainEvent
import depromeet.hotsix.obrit.global.log.analytics.service.AnalyticsEventService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AnalyticsEventListener(private val analyticsEventService: AnalyticsEventService) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleSignupCompleted(event: SignupCompletedDomainEvent) {
        try {
            analyticsEventService.publishSignupCompleted(
                userId = event.userId,
                signupMethod = event.signupMethod,
            )
        } catch (t: Throwable) {
            log.error("signup_completed 이벤트 발행에 실패했습니다.", t)
        }
    }
}
