-- V6: 푸시 알림 기기 등록 및 알림 테이블 추가

-- 푸시 알림 대상 기기. fid(Firebase Installation ID)는 앱 설치 단위 식별자이며
-- 한 기기는 동시에 한 사용자에게만 속하므로 unique.
CREATE TABLE device_registrations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    fid VARCHAR(128) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_registrations_fid (fid),
    INDEX idx_device_registrations_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 발송된 알림 이력. 묶음 발송 정책상 1행 = 소모품 1건이 아니라 발송 1회 단위다.
CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(255) NOT NULL,
    is_read BIT(1) NOT NULL,
    read_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_notifications_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;