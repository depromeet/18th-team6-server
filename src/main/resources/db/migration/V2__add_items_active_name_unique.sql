-- items 테이블에 active_name generated column 추가
-- deleted_at IS NULL(활성 상태)인 경우에만 name 값을 가지고, 삭제된 경우 NULL을 반환한다.
-- MySQL은 partial unique index를 지원하지 않으므로 generated column + UNIQUE(user_id, active_name)으로 대체한다.
-- NULL은 UNIQUE 제약 대상에서 제외되는 MySQL 특성을 활용해 삭제된 항목은 중복 허용된다.
ALTER TABLE items
    ADD COLUMN active_name VARCHAR(255) GENERATED ALWAYS AS (IF(deleted_at IS NULL, name, NULL)) STORED,
    ADD UNIQUE INDEX uq_items_user_active_name (user_id, active_name);