-- 사용자별 발송 시각에 따른 반복 배치에서 정책 알림의 당일 중복 발송을 막는다.
-- 기존 발송 시각과 유형별 설정은 그대로 유지한다.
ALTER TABLE user_notification_settings
    ADD COLUMN last_dispatch_date DATE NULL;
