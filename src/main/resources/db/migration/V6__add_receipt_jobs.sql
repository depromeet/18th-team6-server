CREATE TABLE receipt_jobs
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    image_key     VARCHAR(512) NOT NULL,
    mime_type     VARCHAR(100) NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    result_json   TEXT         NULL,
    error_message VARCHAR(500) NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    deleted_at    DATETIME(6)  NULL,
    PRIMARY KEY (id),
    INDEX idx_receipt_jobs_status_id (status, id)
);