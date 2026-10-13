package depromeet.hotsix.obrit.notification.entity

/** 발송하지 않고 집계만 한 배치 미리보기 결과. */
data class NotificationPreviewSnapshot(
    val targetUserCount: Int,
    val targetItemCount: Int,
    /** 대상이 둘 이상이라 묶음 알림으로 나가는 사용자 수. */
    val bundledUserCount: Int,
    val countByType: Map<NotificationType, Int>,
    val samples: List<Sample>,
) {
    data class Sample(val userId: Long, val itemCount: Int, val title: String, val body: String)
}
