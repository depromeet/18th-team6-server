# PRD v2.1 / 04: 알림 일정과 사유 결합

상태: 설계 초안, 구현 전. 03 다음에 한다.

## 목표

교체 알림을 정해진 일정(사전 D-7·D-3·D-1, 당일, 지연 D+1·D+3·D+7)에 보낸다.
같은 날 같은 소모품에 일정 알림과 여분 부족이 겹치면 하나만 고르지 않고 한 알림으로 합친다. 각 사유는 자기 유형 토글을 따른다.

## 현재 구현

- `NotificationPolicyService`는 소모품마다 `OVERDUE > LOW_STOCK > PRE_REPLACEMENT` 중 **하나만** 고른다. 상위 유형이 꺼져 있으면 아래 유형으로 내려간다.
- 사전 알림: `daysUntil == user.leadDays`일 때 1회. 당일 알림은 따로 없다.
- 지연 알림: 전역 `overdue_step_days`('1,4,7')를 `items.overdue_notified_count`(다음 스텝 위치)와 `last_overdue_notified_at`으로 따라간다. 경과일 `>=` 기준과 스텝 간 최소 간격으로 놓친 날을 따라잡는다.
- 여분 부족: `quantity == 0 && daysUntil <= leadDays && low_stock_notified_at IS NULL`. 재입고 전 1회.
- 발송(`NotificationDispatchService`): 사용자당 푸시 1건. 알림함 `Notification` 1행, `type = 가장 급한 후보의 type`. 소모품이 하나면 `itemId`, `label`, `nextReplacementDate`를 남긴다. 전송 성공(또는 기기 없음) 뒤에만 `recordSent`로 지연 스텝과 여분 부족 기록을 확정한다.
- 전역 `notification_settings.lead_days`(3)는 행이 없는 사용자의 선행 일수 기본값으로만 쓰인다.

## 변경 내용

### 일정

| 시점 | 사유 | 유형 토글 | 판정 |
| --- | --- | --- | --- |
| D-7, D-3, D-1 | 사전 | 교체 예정 (`PRE_REPLACEMENT`) | `daysUntil`이 사전 스텝 목록에 있으면 |
| 당일 (D-0) | 당일 | 교체 예정 (`PRE_REPLACEMENT`) | 사전 스텝 목록의 `0` |
| D+1, D+3, D+7 | 지연 | 교체 지연 (`OVERDUE`) | 기존 스텝 상태 머신 유지 |
| 교체일 3일 전부터 | 여분 부족 | 여분 부족 (`LOW_STOCK`) | 현행 조건, 기준 3일 |

- 사전·당일은 상태를 저장하지 않고 정확히 일치하는 날에만 보낸다. 하루를 놓치면 그 회차는 건너뛰고 다음 회차에 보낸다. 사전은 회차 간격이 짧아 따라잡기를 두지 않는다.
- 지연 스텝을 '1,4,7'에서 '1,3,7'로 바꾼다. `overdue_notified_count`는 스텝 위치라서 진행 중인 소모품도 그대로 이어진다. 예: D+1을 받은 소모품은 다음 스텝이 D+3이 된다.
- 여분 부족 기준 3일은 사용자가 바꿀 수 없다. 전역 값으로 두고 어드민에서만 바꾼다.

### 사유 결합

- 한 소모품의 사유는 일정 사유 최대 1개(사전·당일과 지연은 날짜상 겹치지 않는다)와 여분 부족 최대 1개다.
- 각 사유를 `전역 킬 스위치 AND 유저 유형 토글`로 따로 거른다. 남은 사유가 없으면 후보가 아니다.
- 우선순위로 하나를 고르던 로직과 "상위가 꺼지면 아래로 내려가기" 설명은 삭제한다. 결합하면 꺼진 사유만 빠지므로 내려갈 필요가 없다.
- `NotificationCandidate.type` 대신 `reasons: List<NotificationType>`을 둔다. 순서는 일정 사유, 여분 부족. 대표 유형 `primaryType`은 `reasons.first()`다.
- 묶음 정렬(`daysUntil` 오름차순)과 사용자당 푸시 1건은 유지한다.

### 저장과 상태 확정

- 알림함 `Notification.type`은 컬럼 하나를 유지하고 대표 유형을 저장한다. 묶음이면 가장 급한 소모품의 대표 유형이다 (현행과 같은 규칙).
- `recordSent`는 후보의 사유마다 실행한다. 지연이면 스텝 기록, 여분 부족이면 여분 부족 기록. 사전·당일은 기록하지 않는다.

### 라벨과 문구

라벨(`label`, 최대 50자):

| 사유 | 라벨 |
| --- | --- |
| 사전 | `교체 D-3` |
| 당일 | `교체 D-day` |
| 지연 | `교체 D+3` |
| 여분 부족만 | `여분 부족` |
| 일정 + 여분 부족 | `교체 D-3 · 여분 부족` |

단건 푸시 문구 초안이다. 기능명세서에 문구가 있으면 그 문구를 따른다.

| 사유 | 제목 | 본문 |
| --- | --- | --- |
| 사전 | 교체 시기가 다가와요 | {이름} 교체가 얼마 남지 않았어요 (현행) |
| 당일 | 오늘 교체할 날이에요 | {이름} 교체 예정일이 오늘이에요 |
| 지연 | 교체 시기가 지났어요 | {이름} 교체 예정일이 지났어요 (현행) |
| 여분 부족만 | 여분이 부족해요 | {이름} 여분이 없어요. 지금 준비해두세요 (현행) |
| 일정 + 여분 부족 | 일정 사유의 제목 | 일정 사유 본문 + " 여분도 없어요" |

묶음 문구는 현행 유지.

### 전역 설정과 어드민

마이그레이션(Flyway 다음 번호, 예: `V14__notification_schedule_steps.sql`):

```sql
ALTER TABLE notification_settings
    ADD COLUMN pre_replacement_step_days VARCHAR(50) NOT NULL DEFAULT '7,3,1,0';

ALTER TABLE notification_settings
    RENAME COLUMN lead_days TO low_stock_lead_days;

-- 어드민에서 바꾼 값은 건드리지 않는다.
UPDATE notification_settings SET overdue_step_days = '1,3,7' WHERE overdue_step_days = '1,4,7';
```

- `NotificationSettings`: `preReplacementStepDays`(기본 '7,3,1,0', `preReplacementSteps()` 파싱), `leadDays` → `lowStockLeadDays`, `DEFAULT_OVERDUE_STEP_DAYS = "1,3,7"`.
- 사전 스텝 검증: 0~30, 중복 없음, 최대 5개. 저장 시 내림차순으로 정규화한다. 지연 스텝 검증은 현행 유지.
- 어드민 화면(`templates/admin/notifications.html`): '사전 알림 선행 일수' 입력을 '여분 부족 기준 일수'로 바꾸고 '사전 알림 스텝' 입력을 추가한다.
- 어드민 미리보기 `countByType`은 사유 기준으로 센다. 결합 알림은 두 유형에 모두 잡히므로 합계가 대상 소모품 수보다 클 수 있다. 화면에 그 설명을 한 줄 붙인다.

## 파일과 설정

| 파일 | 변경 |
| --- | --- |
| `src/main/resources/db/migration/V{다음}__notification_schedule_steps.sql` | 신규 |
| `notification/entity/NotificationSettings.kt` | 사전 스텝, 여분 부족 기준, 지연 기본값 |
| `notification/entity/NotificationCandidate.kt` | `reasons`, `primaryType`, `label()` |
| `notification/entity/NotificationType.kt` | KDoc의 "LOW_STOCK만 발송" 설명 갱신 |
| `notification/service/NotificationPolicyService.kt` | 일정 판정, 사유별 필터, KDoc |
| `notification/service/NotificationDispatchService.kt` | 문구, 대표 유형 저장, 사유별 `recordSent`, 미리보기 집계 |
| `notification/service/NotificationSettingsService.kt` | 검증과 저장 |
| `admin/dto/AdminNotificationDto.kt`, `admin/service/AdminNotificationService.kt`, `templates/admin/notifications.html` | 폼과 화면 |

## 범위 밖

- 같은 날 배치를 수동으로 다시 실행하면 사전·당일 알림이 한 번 더 나간다. 현행 사전 알림과 같은 동작이며 이번에 고치지 않는다.
- 사용자별 알림 시간 발송, 알림함 API 변경.

## 완료 기준

- 판정 테스트: D-7·D-3·D-1·당일에 교체 예정 사유, D-2 등 그 밖의 날에는 없음. 지연은 D+1·D+3·D+7과 따라잡기·최소 간격이 현행처럼 동작.
- 결합 테스트: D-3 + 여분 0이면 후보 1건에 사유 2개, 라벨 `교체 D-3 · 여분 부족`. 여분 부족 토글만 꺼지면 사전 사유만 남는다. 교체 예정 토글만 꺼지면 여분 부족만 남는다.
- 발송 테스트: 결합 후보 발송 후 여분 부족 기록이 남고, 지연 결합이면 지연 스텝도 한 칸 진행된다. 전송 실패면 둘 다 기록되지 않는다.
- 마이그레이션: 기존 '1,4,7'이 '1,3,7'로, 다른 값은 유지된다. 로컬 MySQL 컨테이너에서 확인.
- 어드민 화면에서 사전 스텝과 여분 부족 기준을 저장·조회할 수 있다.
- 영향 테스트 갱신: `NotificationPolicyServiceTest`, `OverdueStepComparisonTest`, `NotificationDispatchServiceTest`, `DispatchFailureRetryTest`, `NotificationSettingsServiceTest`, `AdminNotificationServiceTest`, `SpareQuantityNullableTest`. `./gradlew harness` 통과.
