package depromeet.hotsix.obrit.notification.repository

import depromeet.hotsix.obrit.notification.entity.DeviceRegistration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface DeviceRegistrationRepository : JpaRepository<DeviceRegistration, Long> {
    fun findByFid(fid: String): DeviceRegistration?

    fun findAllByUserId(userId: Long): List<DeviceRegistration>

    @Query("select count(distinct d.userId) from DeviceRegistration d")
    fun countDistinctUserId(): Long

    @Query("select distinct d.userId from DeviceRegistration d")
    fun findDistinctUserIds(): List<Long>

    fun deleteByFidAndUserId(fid: String, userId: Long): Long
}
