-- V9: 유저별 알림 설정 테이블 추가
-- 전역 notification_settings(V8)는 기본값 레이어로 남고, 행이 없는 사용자는 전역 값으로 동작한다.
-- 권한 상태는 유저당 단일 행이고 조회 경로가 같아 별도 테이블을 두지 않는다.

CREATE TABLE user_notification_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    enabled BIT(1) NOT NULL DEFAULT b'1',
    pre_replacement_enabled BIT(1) NOT NULL DEFAULT b'1',
    overdue_enabled BIT(1) NOT NULL DEFAULT b'1',
    low_stock_enabled BIT(1) NOT NULL DEFAULT b'1',
    lead_days INT NOT NULL DEFAULT 3,
    dispatch_time TIME NOT NULL DEFAULT '09:00:00',
    permission_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUESTED',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_notification_settings_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;