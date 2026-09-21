package depromeet.hotsix.obrit.notification.entity

/**
 * 기기 알림 권한 상태. 클라이언트가 권한 요청 응답 직후와 앱 실행 시 보고한다.
 *
 * 기기 등록(FID) 유무만으로는 거부와 미요청이 구분되지 않아, 거부 사용자군을 대조군으로 쓸 수 없다.
 */
enum class NotificationPermissionStatus {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
}
