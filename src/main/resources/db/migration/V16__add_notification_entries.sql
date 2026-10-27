-- 한 번의 푸시 안에 포함된 소모품별 알림함 카드.
-- 기존 notifications 컬럼은 호환을 위해 유지하고 신규 알림부터 함께 저장한다.
CREATE TABLE notification_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    notification_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    item_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(255) NOT NULL,
    item_name VARCHAR(255) NULL,
    label VARCHAR(50) NULL,
    next_replacement_date DATE NULL,
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_entries_notification
        FOREIGN KEY (notification_id) REFERENCES notifications (id) ON DELETE CASCADE,
    INDEX idx_notification_entries_notification_order (notification_id, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
