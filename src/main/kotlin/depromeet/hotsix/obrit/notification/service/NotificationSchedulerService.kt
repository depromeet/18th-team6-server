package depromeet.hotsix.obrit.notification.service

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * 정책 알림 배치의 자동 실행 진입점.
 *
 * `notification.schedule.enabled=true`일 때만 빈으로 등록된다. 기본값은 꺼짐이며,
 * 기기 등록(FID) 커버리지를 확인하고 공지를 먼저 내보낸 뒤 수동으로 켜는 것을 전제한다.
 * 스위치가 꺼져 있어도 [NotificationDispatchService]는 살아 있으므로 어드민에서 수동 실행할 수 있다.
 */
@Service
@ConditionalOnProperty(name = ["notification.schedule.enabled"], havingValue = "true")
class NotificationSchedulerService(private val notificationDispatchService: NotificationDispatchService) {

    @Scheduled(cron = "\${notification.schedule.cron:0 0 9 * * *}", zone = "\${notification.schedule.zone:Asia/Seoul}")
    fun run() {
        notificationDispatchService.dispatch()
    }
}
