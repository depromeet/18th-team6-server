package depromeet.hotsix.obrit.notification.repository

import depromeet.hotsix.obrit.notification.entity.UserNotificationSettings
import org.springframework.data.jpa.repository.JpaRepository

interface UserNotificationSettingsRepository : JpaRepository<UserNotificationSettings, Long> {

    fun findByUserId(userId: Long): UserNotificationSettings?

    fun findByUserIdIn(userIds: Collection<Long>): List<UserNotificationSettings>
}
