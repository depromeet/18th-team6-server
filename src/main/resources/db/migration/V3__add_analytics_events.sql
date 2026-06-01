-- V3: users 활성 UUID unique 제약 및 analytics_events 테이블 추가
-- 비즈니스 의미 있는 행동을 명시 발행으로 적재하는 테이블 (Spec 02)

-- 활성(미삭제) 회원에 대해서만 uuid unique. soft delete된 row는 NULL로 인덱스에서 제외됨.
-- MySQL 8.0.13+ functional index 사용.
ALTER TABLE users
    ADD UNIQUE KEY uk_users_active_uuid (
        (CASE WHEN deleted_at IS NULL THEN uuid END)
    );

CREATE TABLE analytics_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL,
    event_name VARCHAR(100) NOT NULL,
    user_id BIGINT,
    occurred_at DATETIME(6) NOT NULL,
    properties JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_analytics_events_event_id (event_id),
    INDEX idx_analytics_events_name_occurred (event_name, occurred_at),
    INDEX idx_analytics_events_user_occurred (user_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
