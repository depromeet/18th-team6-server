package depromeet.hotsix.obrit.global.log.analytics.event

data class SignupCompletedDomainEvent(val userId: Long, val signupMethod: String)
