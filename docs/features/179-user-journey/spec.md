# 179 - 사용자 저니 분석 지표 수집

> **상태**: Draft
> **작성일**: 2026-09-06
> **이슈**: #179

## 1. 개요

사용자 저니 분석에 필요한 지표를 수집하고 `/admin/analytics/user-journey` 화면에서 확인한다.

측정하려는 것은 11가지다.

| # | 지표 |
|---|---|
| 1 | 유저별 최초 로그인 |
| 2 | 온보딩 화면 (= 다건 등록) |
| 3 | 물건 직접 등록 / 영수증 등록 |
| 4 | 영수증 처리 시간·실패 유형 |
| 5 | 홈·목록·검색·상세 조회 |
| 6 | 교체 완료 |
| 7 | 여분 수정 |
| 8 | ~~소모품 등록 완료율~~ (보류, §8 참고) |
| 9 | 30일 재방문율 |
| 10 | 영수증 등록 여부 |
| 11 | 핵심 액션 수행 여부 (등록·편집·교체·여분 수정·삭제) |

## 2. 수집 방식 판단

기존 `AccessLogInterceptor`가 모든 HTTP 요청을 `api_access_logs`에 자동 적재하고 있다.
따라서 **"수집할 것인가"가 아니라 "이미 들어오는 데이터로 답이 나오는가"** 를 먼저 따진다.

### 판단 기준

> `pathTemplate` + `statusCode`만으로 답이 나오면 → 새 이벤트를 만들지 않는다.
> 추가 맥락(왜 실패했나, 몇 건을 처리했나, 무엇과 연결되나)이 필요하면 → `analytics_events`.

같은 사실을 두 테이블에 중복 적재하지 않는다. 어긋났을 때 어느 쪽이 맞는지 판단하는 비용만 생긴다.

### 지표별 결론

| # | 지표 | 근거 | 신규 수집 |
|---|---|---|---|
| 1 | 최초 로그인 | `users.created_at`, `signup_completed` | — |
| 2 | 온보딩 | `POST /items/bulk` | — |
| 3 | 직접/영수증 등록 | `POST /items`, `POST /receipts/analyze` | — |
| 5 | 홈·목록·상세 | `/home/*`, `GET /items`, `GET /items/{itemId}` | — |
| 6 | 교체 완료 | `POST /items/{itemId}/replacements` | — |
| 7 | 여분 수정 | `PATCH /items/{itemId}/spare-count` | — |
| 9 | 30일 재방문율 | `(user_id, occurred_at)` 인덱스 | — |
| 10 | 영수증 등록 여부 | `POST /receipts/analyze` 성공 이력 | — |
| 11 | 핵심 액션 | 각 액션의 고유 `pathTemplate` | — |
| 4 | 영수증 처리 시간·실패 유형 | access log는 `statusCode`만 남음 | ✅ |
| 5 | 검색 | 서버에 검색 API가 없음 | 보류 (§8) |
| 8 | 등록 완료율 | — | 보류 (§8) |

### 신규 수집이 필요한 이유

**4. 영수증 실패 유형** — 현재 `ReceiptOcrClient`가 5xx·타임아웃·응답 파싱 실패·네트워크 오류를 전부
`BusinessException` 하나로 던지고 문자열 메시지로만 구분한다. access log에는 `statusCode`만 남아
"왜 실패했는지"를 사후에 복원할 수 없다.

**4. 영수증 처리 시간** — access log의 `durationMs`에는 이미지 업로드 수신과 응답 직렬화가 포함된다.
OCR 자체 소요 시간과 다르고, 특히 실패는 파일 검증에서 즉시 끊기는 경우와 OCR 타임아웃까지 가는 경우의
차이가 커서 같은 기준으로 재야 성공/실패 비교가 성립한다.

**5. 검색** — `GET /items`는 `userId`만 받고 검색 파라미터가 없다(`ItemController.kt:34`).
클라이언트가 전체 목록을 받아 자체 필터링하므로 서버 로그에 흔적이 남지 않는다.
서버가 관측할 수 없는 유일한 항목이라 이번 범위에서는 수집하지 않는다(§8).

## 3. 이벤트 명세

### 3.1 명명 규칙

현재 코드(`signup_completed`)를 따라 **평면 snake_case**를 쓴다.

> `docs/analytics/event-catalog.md`는 `{domain}.{entity}.{action}` 계층형과 S3→BigQuery 적재를
> 제안하지만, 데이터 파이프라인 학습용으로 정리해둔 참고 자료다. 이 프로젝트는 DB(`analytics_events`)
> 저장 방식으로 간다.

이름은 **과거형**을 쓴다. 이벤트는 이미 일어난 사실이다.

### 3.2 공통 스키마

기존 `analytics_events` 테이블을 그대로 쓴다. 스키마 변경 없음.

| 컬럼 | 설명 |
|---|---|
| `event_id` | UUID. 중복 제거 키 |
| `event_name` | 아래 정의된 이름만 허용 |
| `user_id` | 행동 주체 |
| `occurred_at` | 발생 시각 |
| `properties` | JSON. 이벤트별 속성 |

### 3.3 신규 이벤트

#### `receipt_analysis_finished`

성공·실패를 **한 이벤트로** 기록한다. 성공률과 소요 시간 분포를 한 쿼리로 보는 것이 주 용도이므로,
이벤트를 둘로 나누면 집계할 때마다 다시 합쳐야 한다.

- **트리거**: 영수증 분석이 끝났을 때 (성공·실패 무관)
- **발행 위치**: `ReceiptService.analyzeReceipt`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `success` | boolean | ✅ | 분석 성공 여부 |
| `total_ms` | long | ✅ | 검증~완료(또는 실패)까지 전체 소요 시간 |
| `ocr_ms` | long | ❌ | OCR 호출 구간 소요 시간. OCR 진입 전 실패면 null |
| `detected_item_count` | int | ❌ | 인식된 품목 수. 실패면 null |
| `failure_reason` | enum | ❌ | 실패 사유. 성공이면 null |

**`failure_reason` 값**

| 값 | 조건 |
|---|---|
| `INVALID_FILE` | 확장자·크기·빈 파일 검증 실패 (OCR 호출 전) |
| `UPSTREAM_5XX` | `HttpServerErrorException` |
| `TIMEOUT` | 읽기/연결 타임아웃 |
| `EMPTY_RESPONSE` | 응답 본문 또는 분석 결과 없음 |
| `UNKNOWN` | 그 외 |

**집계 예시**

```sql
-- 성공률 + 성공/실패별 평균 소요 시간을 한 번에
select
    json_value(properties, '$.success') as success,
    count(*) as cnt,
    avg(cast(json_value(properties, '$.total_ms') as unsigned)) as avg_total_ms
from analytics_events
where event_name = 'receipt_analysis_finished'
group by json_value(properties, '$.success');
```

> 사용자 입력값(파일명, 원본 에러 메시지)은 properties에 넣지 않는다. PII 유입 경로가 된다.
> 실패 사유는 위 enum으로만 기록한다.

## 4. 적재 방식

기존 `signup_completed`와 동일한 3단계를 유지한다.

```
도메인 서비스
  ↓ ApplicationEventPublisher.publishEvent()
@TransactionalEventListener(AFTER_COMMIT)
  ↓
AnalyticsEventService.publish()  ── REQUIRES_NEW
```

비즈니스 트랜잭션이 롤백되면 이벤트도 남지 않아야 하므로 `AFTER_COMMIT`이 핵심이다.
이벤트 저장 실패가 본 요청을 깨뜨리지 않도록 리스너에서 예외를 격리한다.

### 영수증 이벤트의 예외

`receipt_analysis_finished`는 위 흐름을 따르지 않는다. 영수증 분석은 DB 트랜잭션이 아니라
외부 API 호출이고, 실패 경로는 예외로 빠져나가 `AFTER_COMMIT`이 걸리지 않는다.
따라서 성공·실패 모두 **`ReceiptService`에서 직접 발행**한다.

이벤트 저장 실패가 영수증 분석 자체를 깨뜨리면 안 되므로, 발행부에서 예외를 격리한다.

## 5. 구조 개선

현재 `AnalyticsEventService`는 이벤트마다 `publishXxx()` 메서드가 하나씩 늘어난다.
이번에 이벤트가 4개 추가되므로 먼저 정리한다.

```kotlin
interface AnalyticsEvent {
    val eventName: AnalyticsEventName
    val userId: Long?
    fun properties(): Map<String, Any?>
}

// Service는 이 하나만
fun publish(event: AnalyticsEvent)
```

- 이벤트명은 `AnalyticsEventName` enum으로 관리해 오타·중복을 막는다
- 새 이벤트는 data class 1개 + enum 상수 1개 추가로 끝난다
- 기존 `publishSignupCompleted()`는 제거하고 `publish()`로 통일한다 (진입점 이원화 방지)

## 6. 관리자 화면

`/admin/analytics/user-journey` 신규 추가. 기존 `signup-funnel`은 그대로 둔다.

가입 후 24시간 코호트를 보는 기존 화면과 30일 단위 지표는 시간 윈도우가 달라
한 화면에 합치면 상단 날짜 필터의 의미가 섹션마다 달라진다.

| 섹션 | 내용 |
|---|---|
| 핵심 액션 도달률 | 등록·편집·교체·여분 수정·삭제 수행 사용자 비율 |
| 영수증 처리 현황 | 성공/실패 건수, 실패 사유 분포, 소요 시간 분포 |
| 30일 재방문율 | 가입 코호트별 재방문 |
| 사용자별 타임라인 | 선택 사용자의 시간순 행동 |

집계는 `AdminBackofficeService.getSignupFunnelJourney()`가 access log로 퍼널을 만드는
기존 패턴을 확장한다.

## 7. 작업 순서

| # | 작업 | 검증 |
|---|---|---|
| 1 | `AnalyticsEvent` 추상화 + enum 도입 | 기존 테스트 2개를 새 API로 수정 후 통과 |
| 2 | `receipt_analysis_finished` 이벤트 | 성공·실패 각각 `success`/`failure_reason`/시간 저장 확인 |
| 3 | user-journey 화면·집계 | 화면 렌더링, 집계 로직 테스트 |

각 단계마다 `./gradlew harness` 통과를 조건으로 한다.

## 8. 범위 밖

- **소모품 등록 완료율 (지표 8)**: `POST /receipts/analyze` → `POST /items/bulk` 전환율.
  정확히 재려면 두 요청을 잇는 키를 클라이언트가 왕복시켜야 해서 앱 팀 합의가 필요하다.
  access log 시간 윈도우로 근사할 수는 있으나 재시도·중간 이탈을 구분하지 못한다.
  다른 지표로 판단이 서지 않을 때 다시 논의한다.
- **온보딩 이탈·화면 진입**: 서버 API를 타지 않는 구간은 클라이언트 이벤트가 필요하다.
  Firebase Analytics로 집계하고, 서버는 관여하지 않는다.
- **검색 (지표 5)**: 검색이 클라이언트 필터링이라 서버에 요청 자체가 도달하지 않는다.
  수집하려면 클라이언트가 별도 API로 보고해야 하는데, 앱 팀이 호출해줘야만 지표가 쌓인다.
  화면 진입·탭과 같은 성격이라 Firebase Analytics 쪽이 맞다.
  서버에서 보려면 `GET /items`에 검색 파라미터를 추가해 검색을 서버로 옮기는 게 선행이다.

## 9. 보류 중인 결정

없음. `ocr_ms`는 `OcrService`가 `ReceiptOcrClient`에 그대로 위임하는 얇은 계층이라
`ReceiptService`에서 호출 전후로 재도 클라이언트 내부와 동일해 그대로 두기로 했다.
