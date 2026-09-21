-- 단건 알림 발송 당시 정보. 기존 이력은 대상 소모품을 복원할 수 없으므로 NULL을 유지한다.
ALTER TABLE notifications
    ADD COLUMN item_id BIGINT NULL,
    ADD COLUMN label VARCHAR(50) NULL,
    ADD COLUMN next_replacement_date DATE NULL;
