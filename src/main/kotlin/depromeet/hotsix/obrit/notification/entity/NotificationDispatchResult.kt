package depromeet.hotsix.obrit.notification.entity

/**
 * 알림 배치 1회의 결과. 사용자 수 기준이다.
 *
 * 실패와 기기없음을 나눠 센다. 둘을 합치면 커버리지가 낮아 못 보낸 것인지 전송이 깨진 것인지 구분되지 않는다.
 */
data class NotificationDispatchResult(val sentUserCount: Int, val failedUserCount: Int, val skippedUserCount: Int)
