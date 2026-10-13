package depromeet.hotsix.obrit.notification.entity

import depromeet.hotsix.obrit.global.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 푸시 알림 대상 기기 등록 정보.
 *
 * FID(Firebase Installation ID)는 앱 설치 단위로 발급되는 안정적인 기기 식별자다.
 * 한 기기는 동시에 한 사용자에게만 속하므로 fid는 unique이며,
 * 한 사용자는 여러 기기를 가질 수 있어 user_id : fid = 1 : N 이다.
 */
@Entity
@Table(
    name = "device_registrations",
    indexes = [Index(name = "idx_device_registrations_user_id", columnList = "user_id")],
    uniqueConstraints = [UniqueConstraint(name = "uk_device_registrations_fid", columnNames = ["fid"])],
)
class DeviceRegistration(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(nullable = false, length = 128)
    var fid: String,
) : BaseTimeEntity() {

    /** 같은 기기에 다른 사용자가 로그인한 경우 소유자를 옮긴다. */
    fun reassignOwner(newUserId: Long) {
        userId = newUserId
    }
}
