-- 로컬/H2 개발용 시드 (DML only). 스키마는 JPA 엔티티 + ddl-auto가 생성한다.
-- 엔티티 @Column 변경 시 이 파일도 함께 수정. INSERT 순서: users → icons → categories (FK)
-- 상세: CLAUDE.md «로컬 시드 데이터 (data.sql)»

INSERT INTO users (name, created_at, updated_at)
VALUES ('dev-user', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO icons (id, name, icon_key, url, created_at, updated_at)
VALUES (1, 'default', 'icons/default-category.png', 'icons/default-category.png', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categories (id, user_id, name, icon_id, default_replacement_interval_days, created_at, updated_at)
VALUES (100, NULL, '면도기', 1, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categories (id, user_id, name, icon_id, default_replacement_interval_days, created_at, updated_at)
VALUES (200, NULL, '제로콜라', 1, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categories (id, user_id, name, icon_id, default_replacement_interval_days, created_at, updated_at)
VALUES (300, NULL, '칫솔', 1, 90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
