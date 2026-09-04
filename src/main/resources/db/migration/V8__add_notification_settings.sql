-- V8: 전역 알림 정책 설정 테이블 추가
-- 운영 중 재배포 없이 값을 조정하기 위해 단일 행(id=1)으로 관리한다.

CREATE TABLE notification_settings (
    id BIGINT NOT NULL,
    auto_dispatch_enabled BIT(1) NOT NULL DEFAULT b'0',
    lead_days INT NOT NULL DEFAULT 3,
    overdue_step_days VARCHAR(50) NOT NULL DEFAULT '1,4,7',
    pre_replacement_enabled BIT(1) NOT NULL DEFAULT b'1',
    overdue_enabled BIT(1) NOT NULL DEFAULT b'1',
    low_stock_enabled BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 자동 발송은 꺼진 상태로 시작한다. 기기 등록 커버리지 확인과 공지 발송 이후 어드민에서 켠다.
INSERT INTO notification_settings (
    id, auto_dispatch_enabled, lead_days, overdue_step_days,
    pre_replacement_enabled, overdue_enabled, low_stock_enabled, created_at, updated_at
) VALUES (
    1, b'0', 3, '1,4,7', b'1', b'1', b'1', NOW(6), NOW(6)
);
