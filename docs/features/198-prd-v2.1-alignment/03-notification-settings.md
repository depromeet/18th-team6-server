# PRD v2.1 / 03: 알림 설정

상태: 구현 완료

## 목표

사용자 알림 설정을 유형별 토글 3개(교체 예정, 교체 지연, 여분 부족)와 공통 알림 시간 하나로 줄인다.
새 사용자는 세 유형 모두 꺼진 상태로 시작한다. 기존 사용자는 이 변경 때문에 받던 알림이 끊기거나 끈 알림을 다시 받지 않는다.

## 현재 구현

- `user_notification_settings`(V9): `enabled`, `pre_replacement_enabled`, `overdue_enabled`, `low_stock_enabled`(모두 기본 1), `lead_days`(기본 3), `dispatch_time`(기본 09:00), `permission_status`.
- 행은 `PUT /notifications/settings`를 처음 호출할 때 생긴다. 행이 없는 사용자는 `EffectiveNotificationSettings.fallback`으로 판정하며, 이때 모든 유형이 켜짐이다.
- 발송 여부는 `전역 유형 킬 스위치 AND enabled AND 유저 유형 토글`이다.
- `dispatchTime`은 30분 단위, 08:00 이상 22:00 미만만 저장할 수 있다. 정책 발송에는 쓰이지 않는다.
- `PUT`은 모든 필드를 받는 전체 교체다.

## 변경 내용

### 기존 사용자 보존 방식

행이 없는 사용자의 판정 기본값을 '꺼짐'으로 바꾸고, **마이그레이션 시점에 존재하는 모든 사용자에게 지금 상태를 행으로 기록**한다. 이후 가입한 사용자만 행이 없고, 행이 없으면 꺼짐이다.
가입 시 설정 행을 만드는 방식보다 사용자 도메인과 알림 도메인 사이의 호출이 늘지 않고, 판정 기본값이 PRD 기본값과 같아진다.

### 스키마 (Flyway 다음 번호, 예: `V13__simplify_user_notification_settings.sql`)

순서가 중요하다.

1. 전체 토글을 꺼둔 사용자는 세 유형을 모두 끈다. 전체 토글이 사라져도 알림을 다시 받지 않게 한다.

   ```sql
   UPDATE user_notification_settings
   SET pre_replacement_enabled = b'0', overdue_enabled = b'0', low_stock_enabled = b'0'
   WHERE enabled = b'0';
   ```

2. `enabled`, `lead_days` 컬럼을 삭제한다. 개인별 선행 일수는 버린다. 사전 알림은 04의 고정 일정을 따른다.
3. 세 토글 컬럼의 DB 기본값을 `b'0'`으로 바꾼다.
4. 행이 없는 기존 사용자에게 지금 판정 결과와 같은 행(세 유형 켜짐, 09:00, `NOT_REQUESTED`)을 넣는다. 탈퇴 처리된 사용자를 포함해도 해가 없으므로 `users` 전체를 대상으로 한다.

   ```sql
   INSERT INTO user_notification_settings
       (user_id, pre_replacement_enabled, overdue_enabled, low_stock_enabled,
        dispatch_time, permission_status, created_at, updated_at)
   SELECT u.id, b'1', b'1', b'1', '09:00:00', 'NOT_REQUESTED', NOW(6), NOW(6)
   FROM users u
   WHERE NOT EXISTS (SELECT 1 FROM user_notification_settings s WHERE s.user_id = u.id);
   ```

마이그레이션은 앱 기동 시 새 코드보다 먼저 적용되므로, 그 사이에 가입한 사용자가 켜짐으로 남는 틈은 없다.

### 엔티티와 판정

- `UserNotificationSettings`: `enabled`, `leadDays` 삭제. 세 토글의 기본값 `false`. 발송 시각 범위 상수(`MIN_DISPATCH_TIME`, `MAX_DISPATCH_TIME_EXCLUSIVE`) 삭제.
- `EffectiveNotificationSettings`: `enabled`, `leadDays` 삭제. `fallback`은 세 유형 모두 `false`. `isEnabled(type)`는 유형 토글만 본다.
- 전역 `notification_settings`의 유형별 킬 스위치는 유지한다.

### API

`GET /notifications/settings` 응답과 `PUT /notifications/settings` 요청:

| 필드 | 변경 |
| --- | --- |
| `enabled` | 요청에서 삭제. 응답은 한동안 `true` 고정 |
| `leadDays` | 요청에서 삭제. 응답은 한동안 `3` 고정 |
| `preReplacementEnabled`, `overdueEnabled`, `lowStockEnabled` | 유지 |
| `dispatchTime` | 유지. 30분 단위 검증은 유지하고 08:00~21:30 범위 제한은 없앤다 (조용 시간 삭제) |
| `permissionStatus` | 응답에 유지 |

- 저장된 `dispatchTime`은 이번에도 발송에 쓰지 않는다. 발송은 매일 09:00 일괄이다.
- Swagger 설명에서 전체 토글·선행 일수·조용 시간 언급을 지운다.

## 호환성

- 구버전 앱의 `PUT`에 `enabled`, `leadDays`가 있어도 무시되고 성공한다. 구버전 앱에서 전체 토글을 끄면 아무것도 꺼지지 않는다.
- 구버전 앱이 `GET` 응답의 `enabled`, `leadDays`를 필수로 읽으면 화면이 깨질 수 있다. 그래서 한동안 응답에 고정값(`enabled: true`, `leadDays: 3`)을 남기고 다음 배포에서 지운다.

## 파일과 설정

| 파일 | 변경 |
| --- | --- |
| `src/main/resources/db/migration/V{다음}__simplify_user_notification_settings.sql` | 신규 |
| `notification/entity/UserNotificationSettings.kt`, `EffectiveNotificationSettings.kt` | 필드·기본값 |
| `notification/service/UserNotificationSettingsService.kt` | 검증, 저장 |
| `notification/dto/request/UpdateNotificationSettingsRequest.kt`, `dto/response/NotificationSettingsResponse.kt` | 필드 |
| `notification/controller/docs/NotificationSettingsControllerApi.kt` | 설명 |
| `notification/service/NotificationPolicyService.kt` | `user.leadDays` 참조 제거 (04에서 대체) |

## 완료 기준

- 마이그레이션 적용 전후로 기존 사용자의 유형별 수신 판정이 같다. 전체 토글을 꺼둔 사용자는 세 유형 모두 꺼짐이다. 운영과 같은 MySQL 버전의 로컬 컨테이너에서 샘플 데이터로 확인한다.
- 마이그레이션 이후 가입한 사용자는 `GET` 결과 세 유형이 꺼짐이고 정책 알림 대상이 아니다.
- `PUT`이 세 토글과 알림 시간만으로 동작하고, 30분 단위가 아니면 거절한다. 23:00 같은 시간도 저장된다.
- 영향 테스트 갱신: `UserNotificationSettings*Test`, `NotificationPolicyUserSettingsTest`, `NotificationSettingsServiceTest`. `./gradlew harness` 통과.
