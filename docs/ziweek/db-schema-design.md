# DB Schema Design — 소모품 트래커 (OBRIT)

## 개요

소모품 트래커 앱으로, 사용자가 칫솔/샴푸 등 일상 소모품을 등록하고 교체 주기를 관리할 수 있다.

**핵심 기능:**
- 소모품 등록 및 카테고리 분류
- 날짜 기반 사용량 자동 추적 (등록일 → 예상 수명)
- 교체 임박 알림
- 교체 이력 관리
- 소셜 로그인 (Google, Kakao)

**DB:** MySQL

---

## MVP 테이블 설계

### User

사용자 정보. 소셜 로그인 기반.

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 사용자 ID |
| email | VARCHAR(255) | UNIQUE, NOT NULL | 이메일 |
| nickname | VARCHAR(50) | NOT NULL | 닉네임 |
| provider | VARCHAR(20) | NOT NULL | 소셜 로그인 제공자 (GOOGLE, KAKAO) |
| created_at | DATETIME | NOT NULL, DEFAULT NOW() | 생성일 |
| updated_at | DATETIME | NOT NULL, DEFAULT NOW() | 수정일 |

### Category

소모품 카테고리. 사전 정의된 값으로 운영.

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 카테고리 ID |
| name | VARCHAR(50) | NOT NULL | 카테고리명 (욕실용품, 주방용품, 청소용품 등) |
| created_at | DATETIME | NOT NULL, DEFAULT NOW() | 생성일 |

### Product

제품 템플릿. "칫솔"이라는 제품 자체의 정보.

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 제품 ID |
| name | VARCHAR(100) | NOT NULL | 제품명 |
| category_id | BIGINT | FK → Category(id), NOT NULL | 카테고리 |
| default_lifespan_days | INT | NOT NULL | 기본 교체 주기 (일) |
| created_at | DATETIME | NOT NULL, DEFAULT NOW() | 생성일 |

### Inventory

사용자의 실제 소모품 인스턴스. "내가 3월 1일부터 쓰고 있는 칫솔".

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 인벤토리 ID |
| user_id | BIGINT | FK → User(id), NOT NULL | 사용자 |
| product_id | BIGINT | FK → Product(id), NOT NULL | 제품 |
| started_at | DATE | NOT NULL | 사용 시작일 |
| expected_replacement_date | DATE | NOT NULL | 교체 예정일 (started_at + default_lifespan_days) |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'IN_USE' | 상태: IN_USE, REPLACED, EXPIRED |
| created_at | DATETIME | NOT NULL, DEFAULT NOW() | 생성일 |
| updated_at | DATETIME | NOT NULL, DEFAULT NOW() | 수정일 |

### History

교체/사용 이벤트 이력 로그.

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 이력 ID |
| inventory_id | BIGINT | FK → Inventory(id), NOT NULL | 인벤토리 |
| event_type | VARCHAR(20) | NOT NULL | 이벤트 타입: STARTED, REPLACED |
| created_at | DATETIME | NOT NULL, DEFAULT NOW() | 이벤트 발생일 |

---

## 인덱스 전략

복합 인덱스 컬럼 순서 원칙: **등호 조건 → 범위/정렬 조건** 순서로 배치하여 B-Tree 인덱스 효율을 극대화한다.

### User

| 인덱스 | 컬럼 | 근거 |
|--------|------|------|
| UNIQUE | email | 중복 가입 방지 |
| INDEX | provider | 소셜 로그인 제공자별 사용자 조회 |

### Product

| 인덱스 | 컬럼 | 근거 |
|--------|------|------|
| INDEX | category_id | 카테고리별 제품 목록 조회 |

### Inventory

| 인덱스 | 컬럼 | 근거 |
|--------|------|------|
| INDEX | (user_id, status) | **메인 화면 "내 소모품 목록" 조회.** user_id(등호)로 사용자 데이터를 좁히고, status(등호)로 사용 중인 것만 필터링. 앱에서 가장 자주 호출되는 쿼리. |
| INDEX | (status, expected_replacement_date) | **교체 임박 알림 배치 조회.** status = 'IN_USE'(등호)로 범위를 좁힌 뒤, expected_replacement_date(범위)로 스캔. 알림 스케줄러가 주기적으로 호출. |

### History

| 인덱스 | 컬럼 | 근거 |
|--------|------|------|
| INDEX | (inventory_id, created_at) | **특정 소모품의 교체 이력 시간순 조회.** inventory_id(등호)로 해당 소모품 이력만 찾고, created_at(정렬)으로 ORDER BY까지 인덱스에서 해결하여 filesort 회피. |

---

## 필터링 전략

### 사용자별 소모품 목록 (메인 화면)

```sql
SELECT i.*, p.name, p.default_lifespan_days, c.name AS category_name
FROM inventory i
JOIN product p ON i.product_id = p.id
JOIN category c ON p.category_id = c.id
WHERE i.user_id = ?
  AND i.status = 'IN_USE'
ORDER BY i.expected_replacement_date ASC;
```

사용 인덱스: `INDEX(user_id, status)`

### 교체 임박 소모품 조회 (알림용)

```sql
SELECT i.*, u.email, p.name
FROM inventory i
JOIN user u ON i.user_id = u.id
JOIN product p ON i.product_id = p.id
WHERE i.status = 'IN_USE'
  AND i.expected_replacement_date <= DATE_ADD(NOW(), INTERVAL 7 DAY);
```

사용 인덱스: `INDEX(status, expected_replacement_date)`

### 카테고리별 필터

```sql
SELECT i.*, p.name
FROM inventory i
JOIN product p ON i.product_id = p.id
WHERE i.user_id = ?
  AND p.category_id = ?
  AND i.status = 'IN_USE';
```

### 특정 소모품 교체 이력

```sql
SELECT * FROM history
WHERE inventory_id = ?
ORDER BY created_at DESC;
```

사용 인덱스: `INDEX(inventory_id, created_at)`

---

## MAU 10,000 확장 시 추가 테이블

MVP 안정화 이후, 아래 테이블을 추가하여 기능을 확장한다.

### Notification

교체 알림 스케줄 및 발송 이력.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 알림 ID |
| user_id | BIGINT FK | 대상 사용자 |
| inventory_id | BIGINT FK | 대상 소모품 |
| scheduled_at | DATETIME | 알림 예정 시간 |
| sent_at | DATETIME | 실제 발송 시간 (nullable) |
| status | VARCHAR(20) | PENDING, SENT, FAILED |

### Group

가구/룸메이트 등 소모품 공유 그룹.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 그룹 ID |
| name | VARCHAR(50) | 그룹명 |
| created_at | DATETIME | 생성일 |

### GroupMember

그룹 멤버 매핑.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 매핑 ID |
| group_id | BIGINT FK | 그룹 |
| user_id | BIGINT FK | 사용자 |
| role | VARCHAR(20) | OWNER, MEMBER |
| joined_at | DATETIME | 가입일 |

---

## ERD

```
User (1) ──── (N) Inventory (N) ──── (1) Product (N) ──── (1) Category
                      │
                      │ (1:N)
                      ▼
                   History
```

- User → Inventory: 한 사용자가 여러 소모품을 등록
- Product → Inventory: 한 제품 템플릿으로 여러 인벤토리 생성
- Category → Product: 한 카테고리에 여러 제품
- Inventory → History: 한 소모품에 여러 교체 이력
