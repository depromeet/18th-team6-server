package depromeet.hotsix.obrit.notification.repository

import depromeet.hotsix.obrit.notification.entity.DeviceRegistration
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceRegistrationRepository : JpaRepository<DeviceRegistration, Long> {
    fun findByFid(fid: String): DeviceRegistration?

    fun findAllByUserId(userId: Long): List<DeviceRegistration>

    fun deleteByFidAndUserId(fid: String, userId: Long): Long
}
