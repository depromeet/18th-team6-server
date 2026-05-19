package depromeet.hotsix.obrit.notification.repository

import depromeet.hotsix.obrit.notification.entity.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findAllByUserId(userId: Long): List<Notification>
}
