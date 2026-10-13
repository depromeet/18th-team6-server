package depromeet.hotsix.obrit.notification.entity

import depromeet.hotsix.obrit.global.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "notifications",
    indexes = [Index(name = "idx_notifications_user_created", columnList = "user_id, created_at")],
)
class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: NotificationType,

    @Column(nullable = false)
    var title: String = "",

    @Column(nullable = false)
    var body: String = "",

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "read_at")
    var readAt: LocalDateTime? = null,
    // 단건 알림의 발송 시점 정보. 묶음·공지·기존 알림은 null이다.
    @Column(name = "item_id")
    val itemId: Long? = null,

    @Column(length = 50)
    val label: String? = null,

    @Column(name = "next_replacement_date")
    val nextReplacementDate: LocalDate? = null,
) : BaseTimeEntity() {

    fun markAsRead() {
        if (isRead) return
        isRead = true
        readAt = LocalDateTime.now()
    }
}
