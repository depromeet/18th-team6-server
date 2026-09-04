package depromeet.hotsix.obrit.notification.entity

/** 발송된 알림의 유형. 동일 소모품에 PRE_REPLACEMENT와 LOW_STOCK 조건이 동시 성립하면 LOW_STOCK만 발송한다. */
enum class NotificationType {
    PRE_REPLACEMENT,
    OVERDUE,
    LOW_STOCK,
}
