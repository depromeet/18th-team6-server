package depromeet.hotsix.obrit.global.log.analytics.event

/**
 * analytics_events 테이블에 적재되는 이벤트.
 *
 * 새 이벤트는 이 인터페이스를 구현하고 [AnalyticsEventName]에 상수를 추가한다.
 * api_access_logs의 pathTemplate/statusCode만으로 알 수 있는 사실은 중복 적재하지 않는다.
 */
interface AnalyticsEvent {
    val eventName: AnalyticsEventName
    val userId: Long?

    fun properties(): Map<String, Any?>
}
