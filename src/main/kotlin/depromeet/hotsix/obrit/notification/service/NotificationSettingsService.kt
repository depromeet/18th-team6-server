package depromeet.hotsix.obrit.notification.service

import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.notification.entity.NotificationSettings
import depromeet.hotsix.obrit.notification.repository.NotificationSettingsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationSettingsService(private val notificationSettingsRepository: NotificationSettingsRepository) {

    /** 설정 행이 없으면 기본값으로 만들어 반환한다. 마이그레이션으로 시드되지 않은 환경도 동작해야 하기 때문이다. */
    @Transactional
    fun current(): NotificationSettings = notificationSettingsRepository
        .findById(NotificationSettings.SINGLETON_ID)
        .orElseGet { notificationSettingsRepository.save(NotificationSettings()) }

    @Transactional
    fun updateAutoDispatch(enabled: Boolean): NotificationSettings = current().apply {
        autoDispatchEnabled = enabled
    }

    @Transactional
    fun update(
        leadDays: Int,
        overdueStepDays: String,
        preReplacementEnabled: Boolean,
        overdueEnabled: Boolean,
        lowStockEnabled: Boolean,
    ): NotificationSettings {
        validateLeadDays(leadDays)
        val normalizedSteps = normalizeOverdueStepDays(overdueStepDays)

        return current().apply {
            this.leadDays = leadDays
            this.overdueStepDays = normalizedSteps
            this.preReplacementEnabled = preReplacementEnabled
            this.overdueEnabled = overdueEnabled
            this.lowStockEnabled = lowStockEnabled
        }
    }

    private fun validateLeadDays(leadDays: Int) {
        if (leadDays !in NotificationSettings.MIN_LEAD_DAYS..NotificationSettings.MAX_LEAD_DAYS) {
            throw BusinessException(
                "사전 알림 선행 일수는 ${NotificationSettings.MIN_LEAD_DAYS}일 이상 " +
                    "${NotificationSettings.MAX_LEAD_DAYS}일 이하여야 합니다.",
            )
        }
    }

    /**
     * 지연 알림 스텝을 검증하고 정규화한다.
     *
     * 간격이 좁으면 며칠 연속으로 알림이 나가 무시 습관을 만들기 때문에 오름차순·중복 없음을 강제한다.
     */
    private fun normalizeOverdueStepDays(raw: String): String {
        val steps = raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.toIntOrNull() ?: throw BusinessException("지연 알림 스텝은 숫자를 쉼표로 구분해 입력해야 합니다.") }

        if (steps.isEmpty()) {
            throw BusinessException("지연 알림 스텝은 최소 1개 이상이어야 합니다.")
        }
        if (steps.size > NotificationSettings.MAX_OVERDUE_STEP_COUNT) {
            throw BusinessException("지연 알림 스텝은 최대 ${NotificationSettings.MAX_OVERDUE_STEP_COUNT}개까지 설정할 수 있습니다.")
        }
        if (steps.any { it !in NotificationSettings.MIN_OVERDUE_STEP_DAY..NotificationSettings.MAX_OVERDUE_STEP_DAY }) {
            throw BusinessException(
                "지연 알림 스텝은 ${NotificationSettings.MIN_OVERDUE_STEP_DAY}일 이상 " +
                    "${NotificationSettings.MAX_OVERDUE_STEP_DAY}일 이하여야 합니다.",
            )
        }
        if (steps != steps.sorted() || steps.distinct().size != steps.size) {
            throw BusinessException("지연 알림 스텝은 중복 없이 오름차순으로 입력해야 합니다.")
        }

        return steps.joinToString(",")
    }
}
