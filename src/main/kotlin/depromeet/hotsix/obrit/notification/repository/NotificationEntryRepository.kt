package depromeet.hotsix.obrit.notification.repository

import depromeet.hotsix.obrit.notification.entity.NotificationEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface NotificationEntryRepository : JpaRepository<NotificationEntry, Long> {

    @Query(
        """
        SELECT entry
        FROM NotificationEntry entry
        WHERE entry.notification.id IN :notificationIds
        ORDER BY entry.notification.id ASC, entry.displayOrder ASC
        """,
    )
    fun findAllByNotificationIds(notificationIds: Collection<Long>): List<NotificationEntry>
}
