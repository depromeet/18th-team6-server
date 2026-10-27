package depromeet.hotsix.obrit.global.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/** 테스트처럼 백그라운드 폴링이 데이터를 가로채면 안 되는 환경에서는 `obrit.scheduling.enabled=false`로 끈다. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "obrit.scheduling", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class SchedulingConfig
