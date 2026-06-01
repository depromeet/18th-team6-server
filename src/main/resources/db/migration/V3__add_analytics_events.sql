-- V3: analytics_events 테이블 추가
-- 비즈니스 의미 있는 행동을 명시 발행으로 적재하는 테이블 (Spec 02)

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
