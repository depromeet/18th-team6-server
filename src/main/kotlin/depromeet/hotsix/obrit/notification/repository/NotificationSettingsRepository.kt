package depromeet.hotsix.obrit.notification.repository

import depromeet.hotsix.obrit.notification.entity.NotificationSettings
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationSettingsRepository : JpaRepository<NotificationSettings, Long>
