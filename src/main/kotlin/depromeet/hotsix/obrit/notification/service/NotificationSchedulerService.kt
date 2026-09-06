package depromeet.hotsix.obrit.notification.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * 정책 알림 배치의 자동 실행 진입점.
 *
 * 실제 발송 여부는 실행 시점에 `notification_settings.auto_dispatch_enabled`로 판단한다.
 * 설정을 DB에 둔 이유는 재배포 없이 어드민에서 켜고 끄기 위해서다. 기본값은 꺼짐이며,
 * 기기 등록(FID) 커버리지 확인과 공지 발송이 선행되는 것을 전제한다.
 * 꺼져 있어도 [NotificationDispatchService]는 어드민에서 수동으로 실행할 수 있다.
 */
@Service
class NotificationSchedulerService(
    private val notificationDispatchService: NotificationDispatchService,
    private val notificationSettingsService: NotificationSettingsService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${notification.schedule.cron:0 0 9 * * *}", zone = "\${notification.schedule.zone:Asia/Seoul}")
    fun run() {
        // 예외가 스케줄러 밖으로 나가면 다음 실행까지 조용히 멈춘 것처럼 보이므로 여기서 삼키고 남긴다.
        // 설정 조회도 같은 범위에 둔다. DB 장애로 조회가 실패해도 원인이 로그에 남아야 한다.
        runCatching {
            if (!notificationSettingsService.current().autoDispatchEnabled) {
                log.info("자동 발송이 꺼져 있어 알림 배치를 건너뛴다.")
                return@runCatching
            }
            notificationDispatchService.dispatch()
        }.onFailure { log.error("알림 배치 실행 실패", it) }
    }
}
