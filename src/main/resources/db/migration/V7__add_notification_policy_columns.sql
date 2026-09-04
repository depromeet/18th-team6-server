-- V7: 알림 정책(#50) 적용을 위한 컬럼 추가

ALTER TABLE items
    ADD COLUMN overdue_notified_count INT NOT NULL DEFAULT 0,
    ADD COLUMN last_overdue_notified_at DATE NULL,
    ADD COLUMN low_stock_notified_at DATE NULL;

ALTER TABLE notifications
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'PRE_REPLACEMENT';

ALTER TABLE notifications
    ALTER COLUMN type DROP DEFAULT;