package depromeet.hotsix.obrit.notification.entity

import depromeet.hotsix.obrit.global.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(
    name = "notification_entries",
    indexes = [
        Index(
            name = "idx_notification_entries_notification_order",
            columnList = "notification_id, display_order",
        ),
    ],
)
class NotificationEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    val notification: Notification,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: NotificationType,

    @Column(name = "item_id")
    val itemId: Long? = null,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val body: String,

    @Column(name = "item_name")
    val itemName: String? = null,

    @Column(length = 50)
    val label: String? = null,

    @Column(name = "next_replacement_date")
    val nextReplacementDate: LocalDate? = null,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,
) : BaseTimeEntity()
