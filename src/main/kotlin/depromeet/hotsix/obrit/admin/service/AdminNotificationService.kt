package depromeet.hotsix.obrit.admin.service

import depromeet.hotsix.obrit.admin.dto.AdminDeviceCoverageRow
import depromeet.hotsix.obrit.admin.dto.AdminNoticeForm
import depromeet.hotsix.obrit.admin.dto.AdminNotificationDashboard
import depromeet.hotsix.obrit.admin.dto.AdminNotificationSettingsForm
import depromeet.hotsix.obrit.admin.dto.AdminNotificationSettingsRow
import depromeet.hotsix.obrit.notification.entity.NotificationSettings
import depromeet.hotsix.obrit.notification.repository.DeviceRegistrationRepository
import depromeet.hotsix.obrit.notification.service.NotificationDispatchService
import depromeet.hotsix.obrit.notification.service.NotificationNoticeService
import depromeet.hotsix.obrit.notification.service.NotificationSettingsService
import depromeet.hotsix.obrit.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminNotificationService(
    private val notificationSettingsService: NotificationSettingsService,
    private val notificationDispatchService: NotificationDispatchService,
    private val notificationNoticeService: NotificationNoticeService,
    private val deviceRegistrationRepository: DeviceRegistrationRepository,
    private val userRepository: UserRepository,
) {

    /**
     * readOnly를 걸지 않는다. [NotificationSettingsService.current]가 설정 행이 없으면 만드는데,
     * readOnly 트랜잭션에 참여하면 flush가 일어나지 않아 INSERT가 예외 없이 사라진다.
     */
    @Transactional
    fun getDashboard(): AdminNotificationDashboard = AdminNotificationDashboard(
        coverage = getCoverage(),
        settings = notificationSettingsService.current().toRow(),
        preview = notificationDispatchService.preview(),
    )

    @Transactional(readOnly = true)
    fun getCoverage(): AdminDeviceCoverageRow = AdminDeviceCoverageRow(
        totalUserCount = userRepository.count(),
        registeredUserCount = deviceRegistrationRepository.countDistinctUserId(),
        deviceCount = deviceRegistrationRepository.count(),
    )

    fun updateSettings(form: AdminNotificationSettingsForm) {
        notificationSettingsService.update(
            leadDays = form.leadDays,
            overdueStepDays = form.overdueStepDays,
            preReplacementEnabled = form.preReplacementEnabled,
            overdueEnabled = form.overdueEnabled,
            lowStockEnabled = form.lowStockEnabled,
        )
    }

    fun updateAutoDispatch(enabled: Boolean) {
        notificationSettingsService.updateAutoDispatch(enabled)
    }

    /** 정책 배치를 지금 실행한다. 자동 발송 스위치와 무관하게 동작한다. */
    fun dispatchNow(): Int = notificationDispatchService.dispatch()

    fun sendNotice(form: AdminNoticeForm): Int {
        val userId = form.userId
        if (userId != null) {
            notificationNoticeService.sendToUser(userId, form.title.trim(), form.body.trim())
            return 1
        }
        return notificationNoticeService.sendToAll(form.title.trim(), form.body.trim())
    }

    private fun NotificationSettings.toRow(): AdminNotificationSettingsRow = AdminNotificationSettingsRow(
        autoDispatchEnabled = autoDispatchEnabled,
        leadDays = leadDays,
        overdueStepDays = overdueStepDays,
        preReplacementEnabled = preReplacementEnabled,
        overdueEnabled = overdueEnabled,
        lowStockEnabled = lowStockEnabled,
    )
}
