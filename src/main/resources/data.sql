INSERT INTO users (id, name, created_at, updated_at)
VALUES (1, 'dev-user', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categories (id, user_id, name, image_url, default_replacement_interval_days, created_at, updated_at)
VALUES (100, NULL, '면도기', '/images/default-category.png', 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categories (id, user_id, name, image_url, default_replacement_interval_days, created_at, updated_at)
VALUES (200, NULL, '제로콜라', '/images/default-category.png', 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO categories (id, user_id, name, image_url, default_replacement_interval_days, created_at, updated_at)
VALUES (300, NULL, '칫솔', '/images/default-category.png', 90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
