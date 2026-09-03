package depromeet.hotsix.obrit.notification

import depromeet.hotsix.obrit.notification.entity.DeviceRegistration

object DeviceRegistrationFixture {

    fun deviceRegistration(userId: Long = 1L, fid: String = "cX8fRzQpS0aBvK2mL9wNdT") = DeviceRegistration(
        userId = userId,
        fid = fid,
    )
}
