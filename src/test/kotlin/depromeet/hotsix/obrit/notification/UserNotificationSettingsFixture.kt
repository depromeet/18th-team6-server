package depromeet.hotsix.obrit.notification

import depromeet.hotsix.obrit.notification.entity.UserNotificationSettings

object UserNotificationSettingsFixture {

    /** 세 유형을 모두 켠 사용자. 설정 행이 없으면 알림을 받지 않으므로 정책 알림을 검증할 때 쓴다. */
    fun allEnabled(userId: Long) = UserNotificationSettings(
        userId = userId,
        preReplacementEnabled = true,
        overdueEnabled = true,
        lowStockEnabled = true,
    )
}
