package depromeet.hotsix.obrit.notification.entity

import depromeet.hotsix.obrit.global.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalTime

/**
 * 유저별 알림 설정. 행이 없는 사용자는 세 유형 모두 꺼진 것으로 판정한다.
 *
 * v2.1 전환 시점의 기존 사용자는 마이그레이션(V13)이 당시 상태로 행을 만들어 두었으므로,
 * 행이 없는 사용자는 그 이후 가입한 사용자뿐이다.
 * 유형별 on/off는 전역 [NotificationSettings]가 킬 스위치다. 전역에서 유형을 끄면 유저가 켜두었어도 발송하지 않는다.
 */
@Entity
@Table(name = "user_notification_settings")
class UserNotificationSettings(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "user_id", nullable = false, unique = true)
    val userId: Long,

    @Column(name = "pre_replacement_enabled", nullable = false)
    var preReplacementEnabled: Boolean = false,

    @Column(name = "overdue_enabled", nullable = false)
    var overdueEnabled: Boolean = false,

    @Column(name = "low_stock_enabled", nullable = false)
    var lowStockEnabled: Boolean = false,

    /** 사용자가 고른 공통 알림 시간. 스케줄 설정 시간대 기준이며 기본값은 09:00이다. */
    @Column(name = "dispatch_time", nullable = false)
    var dispatchTime: LocalTime = DEFAULT_DISPATCH_TIME,

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_status", nullable = false, length = 20)
    var permissionStatus: NotificationPermissionStatus = NotificationPermissionStatus.NOT_REQUESTED,

    /** 정책 알림 발송 완료일. 자동·수동 실행과 시간 변경에 따른 당일 중복을 막는다. */
    @Column(name = "last_dispatch_date")
    var lastDispatchDate: LocalDate? = null,
) : BaseTimeEntity() {

    companion object {
        val DEFAULT_DISPATCH_TIME: LocalTime = LocalTime.of(9, 0)

        /** 발송 시각은 30분 단위 슬롯 배치로 처리한다. */
        const val DISPATCH_TIME_UNIT_MINUTES = 30
    }
}
