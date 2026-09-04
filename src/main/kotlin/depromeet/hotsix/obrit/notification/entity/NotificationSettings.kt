package depromeet.hotsix.obrit.notification.entity

import depromeet.hotsix.obrit.global.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 전역 알림 정책 설정. 운영 중 값을 조정할 수 있도록 단일 행(id=1)으로 관리한다.
 *
 * 열어둔 것은 "값"이고 판정 구조 자체는 코드에 남긴다. 알림 유형, 우선순위(여분 부족 > 사전),
 * 묶음 처리 방식은 여기서 바꿀 수 없다. 유저별 설정이 생기면 이 값이 기본값 레이어가 된다.
 */
@Entity
@Table(name = "notification_settings")
class NotificationSettings(
    @Id
    @Column(name = "id", nullable = false)
    var id: Long = SINGLETON_ID,

    /** 스케줄 자동 발송 여부. 기기 등록 커버리지 확인과 공지 발송 이후 켜는 것을 전제로 기본값은 꺼짐이다. */
    @Column(name = "auto_dispatch_enabled", nullable = false)
    var autoDispatchEnabled: Boolean = false,

    @Column(name = "lead_days", nullable = false)
    var leadDays: Int = DEFAULT_LEAD_DAYS,

    /** 지연 알림 발송 시점을 경과일 기준 오름차순으로 나열한 CSV. 예: "1,4,7" */
    @Column(name = "overdue_step_days", nullable = false, length = 50)
    var overdueStepDays: String = DEFAULT_OVERDUE_STEP_DAYS,

    @Column(name = "pre_replacement_enabled", nullable = false)
    var preReplacementEnabled: Boolean = true,

    @Column(name = "overdue_enabled", nullable = false)
    var overdueEnabled: Boolean = true,

    @Column(name = "low_stock_enabled", nullable = false)
    var lowStockEnabled: Boolean = true,
) : BaseTimeEntity() {

    /**
     * 저장 시 검증하지만, 값이 직접 수정되어 깨져 있어도 조회는 실패하지 않아야 한다.
     * 파싱할 수 없는 값은 버린다. 전부 버려지면 지연 알림이 나가지 않을 뿐 배치와 관리 화면은 계속 동작한다.
     */
    fun overdueSteps(): List<Int> = overdueStepDays.split(",").mapNotNull { it.trim().toIntOrNull() }

    fun isEnabled(type: NotificationType): Boolean = when (type) {
        NotificationType.PRE_REPLACEMENT -> preReplacementEnabled
        NotificationType.OVERDUE -> overdueEnabled
        NotificationType.LOW_STOCK -> lowStockEnabled
    }

    companion object {
        const val SINGLETON_ID = 1L
        const val DEFAULT_LEAD_DAYS = 3
        const val DEFAULT_OVERDUE_STEP_DAYS = "1,4,7"

        const val MIN_LEAD_DAYS = 1
        const val MAX_LEAD_DAYS = 7
        const val MIN_OVERDUE_STEP_DAY = 1
        const val MAX_OVERDUE_STEP_DAY = 30
        const val MAX_OVERDUE_STEP_COUNT = 5
    }
}
