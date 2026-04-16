
## 0. 모니터링 목적

- **조기 감지**: 사용자가 제보하기 전에 우리가 먼저 안다.
- **영향 파악**: 배포/변경 후 시스템이 더 나빠지지 않았음을 수치로 확인한다.
    - 대시보드
    - 알람
- **제품 이해**: 사용자가 어떻게 제품을 쓰는지 데이터로 파악한다.

---

## 1. 데이터 트랙 구분

관측 데이터는 **목적에 따라 2가지 트랙으로 분리**한다.

| 트랙 | 질문 | 대상 | 본 프로젝트 도입 |
| --- | --- | --- | --- |
| **시스템 관측성** | "건강한가?" | 서버, API, DB | O |
| **비즈니스 지표** | "어떻게 쓰이는가?" | 도메인 이벤트 | O |
- 시스템 관측성 : 장애, 에러 등이 없는지 검사
- 비즈니스 지표 : 사용자들이 어떤 항목을 많이 등록하고, 어떤 서비스를 많이 이용하는지 통계 분석

> **경계**: user_id별 개인 행동 추적이 필요해지는 시점이 오면 전문 도구 도입을 검토한다.
그 전까지는 Prometheus(집계)와 MySQL(상세)로 충분하다.
>

---

## 2. SRE 철학 → 모니터링에서 어떤 걸 수집?

### 2.1 SLI (측정 대상)

| 카테고리 | SLI | 측정 방식 |
| --- | --- | --- |
| 가용성 | 성공 응답 비율 | `2xx,3xx,4xx 응답 수 / 전체 응답 수` (5xx만 실패 처리) |
| 지연 | p99 응답시간 | 히스토그램 |
| 인프라 건강성 | DB/Redis 연결 상태 | 커넥션 풀 사용률, ping 응답 |

→ 해당 지표를 위해 RED/USE 지표 등 수집 예정

### 2.2 SLO (목표)

| 항목 | 목표 |
| --- | --- |
| **가용성** | 99.5% |
| **지연 (p99)** | < 1s |
| **지연 (p95)** | < 500ms |

→ 위 숫자는 **초기값으로,** 실제 서비스 운영 하면서 서버에 맞게 유동적으로 변경 필요

### 2.3 에러버짓

- **에러버짓 = 1 - SLO = 0.5%/월**
- 월간 요청의 0.5%까지는 실패해도 된다 (= 허용된 장애 한도).
- **소진 시 규칙**:
    - 80% 소진: 알림 필요할듯?
    - 100% 소진: 알림 필요할듯?

---

## 3. 지표 모델 — 대시보드

### 1) 시스템 관측 — RED (Application Layer)

**Micrometer가 Spring에서 자동 노출하는 메트릭 사용.**

| 지표 | 용도 |
| --- | --- |
| **R**ate | 초당 요청수, 트래픽 패턴 |
| **E**rror | 서버 에러율 (SLI 계산 기반) |
| **D**uration | p50/p95/p99 지연 히스토그램 |

### 2) 시스템 관측 — USE (OS Layer)

cAdvisor, exporter 등으로 메트릭 수집

| 대상 | 도구 | 핵심 지표 |
| --- | --- | --- |
| 컨테이너 | cAdvisor | CPU 사용률, 메모리, 네트워크 |
| 호스트 | node_exporter | Disk, Load Average, File Descriptor |
| MySQL | mysqld_exporter | 커넥션 사용률, slow query, replication lag |
| Redis | redis_exporter | 메모리, hit rate, command 지연 |

### 3) 비즈니스 지표

**목적**: 제품 사용 패턴 파악 → 비즈니스 지표 관측

### 3-1) DB 직접 저장

**용도**: 상세 분석, 통계

- Grafana에서 MySQL을 datasource로 추가
- 대시보드 패널에 SQL 쿼리 직접 작성
- 별도 인프라 불필요

**쿼리 예시**:

```sql
-- 카테고리별 등록 추이 (지난 7일, 일 단위)
SELECT
  DATE(created_at) AS day,
  category,
  COUNT(*) AS cnt
FROM products
WHERE created_at >= NOW() - INTERVAL 7 DAY
GROUP BY day, category
ORDER BY day;
```

### 4) 트랙 선택 기준

| 질문 유형 | 트랙 | 도구 |
| --- | --- | --- |
| "지금 시스템 건강한가?" | RED | Prometheus |
| "인프라 리소스 충분한가?" | USE | Prometheus + exporter |
| "특정 에러가 어떤 문제인지?" | 로그 추적 | Loki  |
| "지난달 카테고리별 등록 추이는?" | DB 직접 → 추후 대시보드 연동 | MySQL → Grafana |

### 5) 수집 범위

- **시스템 분석:** RED + USE 필수 항목만. 과잉 수집 금지.
- **비즈니스 지표:** 제품 결정에 실제 쓰이는 것만. 막연한 "일단 수집"은 금지.
- **로그**: 에러 로그만 수집 대상

---

## 4. 도구 규약

### 1) 스택 선정

| 계층 | 도구 | 용도 |
| --- | --- | --- |
| 메트릭 저장 | Prometheus (단일 인스턴스) | 시스템 관측 |
| 시각화 | Grafana | 모든 대시보드 |
| 분석 조회 | MySQL (직접) | 비즈니스 지표 |
| 알림 | Grafana Alert → Slack/MySQL | 시스템 관측 |
| 로그 | Loki  | 에러 로그 중앙화 + MDC 추적 |

### 2) 대시보드 구성

- 도메인별 대시보드 구성
    - 마이크로텍처아키텍처의 경우 서비스로 구성했는데.. 우리는 도메인별 혹은 단일로 해도 충분할듯
- 전역 대시보드 구성
- API 확정 후 비즈니스 지표 대시보드는 결정하면 좋을듯

### 3) 배포 어노테이션

**목적**: 그래프 위에 "여기서 배포함"을 수직선으로 찍어서 **배포 전후 에러율/지연 변화를 즉시 인지**. 당근도 이 방식으로 롤백 판단을 한다.

**구현 방식**:

1. Grafana에서 Annotations API 토큰 발급
2. 배포 파이프라인(GitHub Actions 등) 마지막 스텝에서 Grafana API 호출:

```yaml
# GitHub Actions 예시
- name: Grafana deployment annotation
  run: |
    curl -X POST "https://grafana.example.com/api/annotations" \
      -H "Authorization: Bearer ${{ secrets.GRAFANA_TOKEN }}" \
      -H "Content-Type: application/json" \
      -d '{
        "tags": ["deployment", "prod", "api"],
        "text": "deploy v${{ github.sha }} by ${{ github.actor }}"
      }'
```

**롤백 판단 루틴**:

- 배포 직후 **5분간 Overall 대시보드 모니터링 필수**
- 에러율/지연이 임계값 초과 시 수동 롤백

---

## 5. 대시보드 정리

### 5.1 Overall 대시보드

- **목적**: 전체 시스템 상태 한눈에, 알림 연결 대상
- **패널 구성** (위에서 아래로):
    1. SLO 현황 (가용성, p99 지연, 에러버짓 소진율)
    2. RED 요약 (서비스 전체의 Rate/Error/Duration)
    3. USE 요약 (CPU/Memory/Disk)
    4. DB/Redis 연결 상태
    5. **배포 어노테이션 오버레이** (모든 시계열 패널에 공통 적용)


### 2) Service 대시보드

→ 상단의 필터를 이용해 전역 → 서비스 대시보드로 이동 가능

→ 개발자가 **자기 서비스만 필터링해서 보기** 가능.

### 2-1) 메인 패널 구성

![RED 메인 패널](https://github.com/user-attachments/assets/a6fda69b-44f5-43fd-91fc-e2b6802b8138)

| 위치 | 색상 | 역할 |
| --- | --- | --- |
| 좌 | 파란색 | **Rate** — 초당 요청수 (ops/s) |
| 중 | 초록색 | **Error** — 성공률 (%, non-5xx 기준) |
| 우 | - | **Duration** — p50/p95/p99 라인 |

### 2-2) 서비스 대시보드 세부

1. **엔드포인트별 RED** — URI 패턴별로 그룹핑된 Rate/Error 테이블
2. **HTTP 5xx Count** — 에러 상세 (status code별 카운트, 발생 시점 highlight)
3. **Slow endpoint TOP 10** — p99 기준 느린 API 순위

---

## 6. 알림 규칙 (최소 구성, 트랙 A 전용)

> **알림은 “시스템 관측”에만 건다.** 비즈니스 지표는 알림 대상이 아님
"깨어날 만한 것만 알림"이 원칙.
>

### 1) 초기 알림 규칙

| 이름 | 조건 | 심각도 | 대응 |
| --- | --- | --- | --- |
| **app-down** | 5분간 앱 heartbeat 없음 | Critical | 즉시 대응 |
| **error-rate-high** | 에러율 5분간 5% 초과 | Critical | 즉시 조사 |

### 2) 알림 메시지 포맷

모든 알림에 **반드시** 포함:

1. 문제 요약 (1줄)
2. 담당자 멘션 (초기엔 본인)
3. Overall 대시보드 바로가기 링크
4. Service 대시보드 바로가기 링크 (해당 서비스로 필터링된 URL)

예시:

```
🚨 [prod-api] 에러율 5% 초과 (5분간 평균 8.3%)
@담당자
👉 Overall 대시보드: https://grafana.../d/prod-api-overall
👉 Service 대시보드: https://grafana.../d/prod-api-service?var-service=api
```

---