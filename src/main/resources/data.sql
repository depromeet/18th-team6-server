INSERT INTO users (id, name, created_at, updated_at)
VALUES (1, 'dev-user', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO icons (id, name, url, created_at, updated_at)
VALUES (1, 'default', '/images/default-category.png', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categories (id, user_id, name, icon_id, default_replacement_interval_days, created_at, updated_at)
VALUES (100, NULL, '면도기', 1, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categories (id, user_id, name, icon_id, default_replacement_interval_days, created_at, updated_at)
VALUES (200, NULL, '제로콜라', 1, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categories (id, user_id, name, icon_id, default_replacement_interval_days, created_at, updated_at)
VALUES (300, NULL, '칫솔', 1, 90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
