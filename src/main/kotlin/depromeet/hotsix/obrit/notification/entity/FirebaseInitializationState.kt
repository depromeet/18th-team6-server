package depromeet.hotsix.obrit.notification.entity

/**
 * Firebase 초기화 결과.
 *
 * 실패해도 애플리케이션은 기동한다. 알림은 부가 기능이라 초기화 하나로 소모품 조회·등록·교체까지
 * 함께 내려가면 안 된다. 대신 상태를 남겨 조용히 안 나가는 상황을 관리 화면에서 알아챌 수 있게 한다.
 */
enum class FirebaseInitializationState {
    /** 초기화를 시도하지 않았다. test 프로파일처럼 Firebase 설정이 아예 없는 환경. */
    NOT_ATTEMPTED,
    INITIALIZED,
    FAILED,
}
