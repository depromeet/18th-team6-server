package depromeet.hotsix.obrit.notification.entity

import depromeet.hotsix.obrit.global.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "fcm_tokens",
    indexes = [
        Index(name = "uk_fcm_tokens_token", columnList = "token", unique = true),
        Index(name = "idx_fcm_tokens_user_id", columnList = "user_id"),
    ],
)
class FcmToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(nullable = false, unique = true)
    var token: String,
) : BaseTimeEntity() {

    fun reassignOwner(newUserId: Long) {
        if (userId != newUserId) {
            userId = newUserId
        }
    }
}
