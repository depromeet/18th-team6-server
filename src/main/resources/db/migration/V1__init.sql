-- V1: 초기 스키마 (baseline)
-- 기존 운영 DB에 이미 존재하는 스키마를 기록용으로 작성.
-- baseline-on-migrate=true, baseline-version=0 설정으로 인해
-- 기존 DB에서는 이 스크립트가 실행되지 않고 baseline으로 마킹됨.

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uuid VARCHAR(36),
    name VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE icons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    icon_key VARCHAR(255) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT,
    name VARCHAR(255) NOT NULL,
    icon_id BIGINT NOT NULL,
    default_replacement_interval_days INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_categories_user_deleted (user_id, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    count INT NOT NULL,
    replacement_interval_days INT NOT NULL,
    last_replaced_date DATE NOT NULL,
    next_replacement_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_items_user_deleted_next (user_id, deleted_at, next_replacement_date),
    INDEX idx_items_category_deleted (category_id, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE item_replacement_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    replaced_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_item_replacement_histories_item_replaced (item_id, replaced_date),
    CONSTRAINT fk_item_replacement_histories_item FOREIGN KEY (item_id) REFERENCES items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
