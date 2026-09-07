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
import java.time.LocalTime

/**
 * 유저별 알림 설정. 행이 없는 사용자는 전역 [NotificationSettings] 기본값으로 동작한다.
 *
 * 값은 전역이 기본값 레이어이고, 유형별 on/off는 전역이 킬 스위치다.
 * 전역에서 유형을 끄면 유저가 켜두었어도 발송하지 않는다.
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

    /** 알림 수신 전체 on/off. 끄면 유형별 설정과 무관하게 발송하지 않는다. */
    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true,

    @Column(name = "pre_replacement_enabled", nullable = false)
    var preReplacementEnabled: Boolean = true,

    @Column(name = "overdue_enabled", nullable = false)
    var overdueEnabled: Boolean = true,

    @Column(name = "low_stock_enabled", nullable = false)
    var lowStockEnabled: Boolean = true,

    /** 사전 알림 선행 일수. 개별 소모품의 경고 임계(D-3)와는 별개 값이다. */
    @Column(name = "lead_days", nullable = false)
    var leadDays: Int = NotificationSettings.DEFAULT_LEAD_DAYS,

    @Column(name = "dispatch_time", nullable = false)
    var dispatchTime: LocalTime = DEFAULT_DISPATCH_TIME,

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_status", nullable = false, length = 20)
    var permissionStatus: NotificationPermissionStatus = NotificationPermissionStatus.NOT_REQUESTED,
) : BaseTimeEntity() {

    companion object {
        val DEFAULT_DISPATCH_TIME: LocalTime = LocalTime.of(9, 0)
    }
}
