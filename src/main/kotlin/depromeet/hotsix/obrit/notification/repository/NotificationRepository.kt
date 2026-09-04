package depromeet.hotsix.obrit.notification.repository

import depromeet.hotsix.obrit.notification.entity.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<Notification>

    fun findByIdAndUserId(id: Long, userId: Long): Notification?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        "UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false",
    )
    fun markAllAsReadByUserId(userId: Long, readAt: LocalDateTime)
}
