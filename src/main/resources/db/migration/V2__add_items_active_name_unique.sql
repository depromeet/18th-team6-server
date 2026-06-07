-- items 테이블에 활성 소모품 이름 중복 방지를 위한 functional unique index 추가 (MySQL 8.0.13+)
-- deleted_at IS NULL(활성 상태)인 경우에만 name이 유니크하도록 보장한다.
-- 삭제된 항목은 IF 표현식이 NULL을 반환하므로 UNIQUE 제약 대상에서 제외된다.
ALTER TABLE items
    ADD UNIQUE INDEX uq_items_user_active_name (user_id, (IF(deleted_at IS NULL, name, NULL)));