-- V10: 여분 미입력 상태 도입 (SR-NOTI-01)
-- 신규 미입력 값은 NULL로 저장할 수 있도록 한다.
-- 기존 0은 명시적 입력인지 미입력인지 구분할 수 없으므로 그대로 보존한다.
ALTER TABLE items MODIFY COLUMN count INT NULL;
