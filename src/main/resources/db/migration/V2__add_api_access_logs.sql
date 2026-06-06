-- V2: api_access_logs 테이블 추가
-- DAU/리텐션 분석용 access log raw 저장 테이블

CREATE TABLE api_access_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT,
    method VARCHAR(10) NOT NULL,
    path VARCHAR(255) NOT NULL,
    path_template VARCHAR(255) NOT NULL,
    status_code INT NOT NULL,
    duration_ms INT NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_api_access_logs_user_occurred (user_id, occurred_at),
    INDEX idx_api_access_logs_path_template_occurred (path_template, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
