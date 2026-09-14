-- V10: 여분 미입력 상태 도입 (SR-NOTI-01)
-- count가 NOT NULL이라 "0개"와 "입력 안 함"이 같은 값이었다.
-- 알림 판정이 count == 0이므로 온보딩만 마친 사용자 전원이 여분 부족 알림 대상이 됐다.

ALTER TABLE items MODIFY COLUMN count INT NULL;

-- 기존 0을 전부 미입력으로 옮긴다(FRD OQ-01 ⑵안).
-- 0이 미입력인지 사용자가 명시한 0인지 구분할 근거가 없다. 잘못 보내는 것보다 안 보내는 쪽이 안전하고,
-- 사용자가 여분을 입력하면 곧바로 복구된다.
UPDATE items SET count = NULL WHERE count = 0;
