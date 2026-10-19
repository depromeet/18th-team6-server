-- V12: 소모품 카테고리(욕실·주방 등) 추가 (PRD v2.1)
-- 기존 categories·items와 같이 FK 없이 ID 컬럼으로 참조한다.

CREATE TABLE category_groups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE categories ADD COLUMN category_group_id BIGINT NULL;
CREATE INDEX idx_categories_group ON categories (category_group_id);

-- '사용자' 카테고리는 마이그레이션이 만든다. 탭 순서는 마지막으로 두고 DB에서 바꿀 수 있다.
-- 기본 소모품용 PRESET 카테고리와 그 배정은 운영 DB에 직접 넣는다.
INSERT INTO category_groups (name, type, display_order, created_at, updated_at)
VALUES ('사용자', 'USER', 1000, NOW(6), NOW(6));

-- 기존 사용자 소모품을 '사용자' 카테고리로 옮긴다.
UPDATE categories SET category_group_id = (SELECT id FROM category_groups WHERE type = 'USER')
WHERE user_id IS NOT NULL;
