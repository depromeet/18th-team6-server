-- V13: 유저 알림 설정을 유형 토글 3개와 알림 시간으로 단순화 (PRD v2.1)
-- 새 사용자는 세 유형 모두 꺼짐으로 시작하고, 기존 사용자는 지금 받던 상태를 유지한다. 아래 순서를 지켜야 한다.

-- 1. 전체 토글을 꺼둔 사용자는 세 유형을 모두 끈다. 전체 토글이 사라져도 알림을 다시 받지 않게 한다.
UPDATE user_notification_settings
SET pre_replacement_enabled = b'0', overdue_enabled = b'0', low_stock_enabled = b'0'
WHERE enabled = b'0';

-- 2. 전체 토글과 개인별 선행 일수를 없애고, 3. 세 토글의 기본값을 꺼짐으로 바꾼다.
ALTER TABLE user_notification_settings
    DROP COLUMN enabled,
    DROP COLUMN lead_days,
    ALTER COLUMN pre_replacement_enabled SET DEFAULT b'0',
    ALTER COLUMN overdue_enabled SET DEFAULT b'0',
    ALTER COLUMN low_stock_enabled SET DEFAULT b'0';

-- 4. 설정 행이 없는 기존 사용자는 지금까지 모든 유형이 켜진 것으로 판정됐다. 그 상태를 행으로 남긴다.
-- 이후 행이 없는 사용자는 이 마이그레이션 뒤에 가입한 사용자뿐이고, 꺼짐으로 판정한다.
INSERT INTO user_notification_settings
    (user_id, pre_replacement_enabled, overdue_enabled, low_stock_enabled,
     dispatch_time, permission_status, created_at, updated_at)
SELECT u.id, b'1', b'1', b'1', '09:00:00', 'NOT_REQUESTED', NOW(6), NOW(6)
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM user_notification_settings s WHERE s.user_id = u.id);
