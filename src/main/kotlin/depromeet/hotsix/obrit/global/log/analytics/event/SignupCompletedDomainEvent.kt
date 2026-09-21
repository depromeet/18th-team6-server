package depromeet.hotsix.obrit.global.log.analytics.event

data class SignupCompletedDomainEvent(override val userId: Long, val signupMethod: String) : AnalyticsEvent {
    override val eventName = AnalyticsEventName.SIGNUP_COMPLETED

    override fun properties(): Map<String, Any?> = mapOf("signup_method" to signupMethod)
}
