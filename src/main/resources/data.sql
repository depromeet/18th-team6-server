-- 로컬/H2 개발용 시드 (DML only). 스키마는 JPA 엔티티 + ddl-auto가 생성한다.
-- 엔티티 @Column 변경 시 이 파일도 함께 수정. INSERT 순서: users → icons → category_groups → categories (FK)
-- 상세: CLAUDE.md «로컬 시드 데이터 (data.sql)»

INSERT INTO users (name, created_at, updated_at)
VALUES ('dev-user', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO icons (id, name, icon_key, url, created_at, updated_at)
VALUES (1, 'default', 'icons/default-category.png', 'icons/default-category.png', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO category_groups (id, name, type, display_order, created_at, updated_at)
VALUES (1, '욕실', 'PRESET', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO category_groups (id, name, type, display_order, created_at, updated_at)
VALUES (2, '주방', 'PRESET', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO category_groups (id, name, type, display_order, created_at, updated_at)
VALUES (3, '사용자', 'USER', 1000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categories (id, user_id, name, icon_id, default_replacement_interval_days, category_group_id, created_at, updated_at)
VALUES (100, NULL, '면도기', 1, 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categories (id, user_id, name, icon_id, default_replacement_interval_days, category_group_id, created_at, updated_at)
VALUES (200, NULL, '제로콜라', 1, 7, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categories (id, user_id, name, icon_id, default_replacement_interval_days, category_group_id, created_at, updated_at)
VALUES (300, NULL, '칫솔', 1, 90, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- notifications와 notification_entries는 발송 시 생성하므로 시드하지 않는다.
-- user_notification_settings.last_dispatch_date는 실제 발송 완료 시 기록한다. 설정 행은 시드하지 않는다.
