package depromeet.hotsix.obrit.notification.entity

/**
 * 발송된 알림의 유형.
 *
 * PRE_REPLACEMENT는 사전(D-7·D-3·D-1)과 당일 알림을 함께 가리킨다.
 * 같은 소모품에 일정 사유(PRE_REPLACEMENT 또는 OVERDUE)와 LOW_STOCK이 함께 성립하면 한 알림으로 합친다.
 * NOTICE는 정책 판정과 무관하게 어드민에서 직접 발송하는 공지다.
 */
enum class NotificationType {
    PRE_REPLACEMENT,
    OVERDUE,
    LOW_STOCK,
    NOTICE,
}
