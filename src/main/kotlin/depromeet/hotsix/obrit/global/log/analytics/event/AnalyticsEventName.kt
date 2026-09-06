package depromeet.hotsix.obrit.global.log.analytics.event

/**
 * analytics_events.event_name 에 저장되는 이벤트 이름.
 *
 * 이름은 과거형 snake_case로 쓴다. 이벤트는 이미 일어난 사실이다.
 */
enum class AnalyticsEventName(val value: String) {
    SIGNUP_COMPLETED("signup_completed"),
    RECEIPT_ANALYSIS_FINISHED("receipt_analysis_finished"),
}
