-- V14: 알림 일정을 사전 D-7·D-3·D-1·당일, 지연 D+1·D+3·D+7로 바꾼다 (PRD v2.1)

-- 사전·당일 알림을 보낼 교체일까지 남은 일수. 0은 당일이다.
ALTER TABLE notification_settings
    ADD COLUMN pre_replacement_step_days VARCHAR(50) NOT NULL DEFAULT '7,3,1,0';

-- 사전 알림 선행 일수는 사전 스텝으로 대체되고, 여분 부족 기준 일수로만 남는다.
ALTER TABLE notification_settings
    RENAME COLUMN lead_days TO low_stock_lead_days;

-- 기본값을 쓰던 환경만 새 기본값으로 바꾼다. 어드민에서 바꾼 값은 건드리지 않는다.
-- overdue_notified_count는 스텝 위치라서 진행 중인 소모품도 그대로 이어진다. 예: D+1을 받았으면 다음은 D+3이다.
UPDATE notification_settings SET overdue_step_days = '1,3,7' WHERE overdue_step_days = '1,4,7';
ALTER TABLE notification_settings
    ALTER COLUMN overdue_step_days SET DEFAULT '1,3,7';
