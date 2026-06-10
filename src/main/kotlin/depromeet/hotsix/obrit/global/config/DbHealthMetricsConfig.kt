package depromeet.hotsix.obrit.global.config

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.boot.health.contributor.Status
import org.springframework.boot.jdbc.health.DataSourceHealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DbHealthMetricsConfig {

    // DataSourceHealthIndicator -> Spring에서 제공하는 클래스로 SELECT 1 같은 검증 쿼리를 실행해 결과를 Status.UP/Status.DOWN으로 반환
    @Bean
    fun dbHealthIndicator(dataSource: DataSource): DataSourceHealthIndicator = DataSourceHealthIndicator(dataSource)

    // MeterBinder를 사용해 게이지에 등록 -> 등록하면 Prometheus가 스크랩할 때마다 해당 메서드를 호출
    // -> 즉 DB 상태를 지속적으로(prometheus 호출 마다) 체크 가능(1 or 0) -> 0일 때 DB 오류 감지
    @Bean
    fun dbHealthGauge(dbHealthIndicator: DataSourceHealthIndicator): MeterBinder = MeterBinder { registry ->
        Gauge.builder("obrit_db_up") {
            if (dbHealthIndicator.health().status == Status.UP) 1.0 else 0.0
        }.register(registry)
    }
}
