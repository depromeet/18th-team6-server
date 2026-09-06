package depromeet.hotsix.obrit.notification.entity

/**
 * 발송된 알림의 유형.
 *
 * 동일 소모품에 PRE_REPLACEMENT와 LOW_STOCK 조건이 동시 성립하면 LOW_STOCK만 발송한다.
 * NOTICE는 정책 판정과 무관하게 어드민에서 직접 발송하는 공지다.
 */
enum class NotificationType {
    PRE_REPLACEMENT,
    OVERDUE,
    LOW_STOCK,
    NOTICE,
}
