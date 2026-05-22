
# 회원 등록 (UUID 기반)

## 1. Overview

시스템은 안드로이드 클라이언트가 전달한 UUID를 기반으로 회원을 생성하거나 조회할 수 있어야 한다.

* 앱 최초 실행 시 호출되는 공개 API 제공
* UUID 기반 회원 생성
* 동일 UUID 중복 회원 생성 방지
* 이미 등록된 UUID인 경우 기존 회원 반환
* 향후 카카오 로그인 등 추가 인증 수단 연동을 고려한 구조로 설계

---

## 2. Functional Requirements

### 2.1 입력 (HTTP Request)

`POST /users`

### Request Body

```json
{
  "type": "uuid",
  "uuid": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Validation

| 필드   | 조건                              |
| ---- | ------------------------------- |
| type | 필수, 현재 `"uuid"`만 허용             |
| uuid | 필수 (type이 `"uuid"`일 때), UUID 형식 |

---

### 2.2 동작

```text
POST /users 호출
  → type 확인 (현재 "uuid"만 지원)
  → UUID 형식 검증
  → UUID로 기존 회원 조회

    존재하면
      → 기존 회원 반환

    존재하지 않으면
      → 신규 회원 생성
      → UUID 저장
      → 회원 정보 반환
```

---

### 2.3 결과

### 성공 응답

```json
{
  "success": true,
  "data": {
    "id": 1,
    "uuid": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

---

## 3. Acceptance Criteria

* [ ] UUID로 회원 등록 API를 호출할 수 있다
* [ ] 존재하지 않는 UUID이면 신규 회원이 생성된다
* [ ] 이미 존재하는 UUID이면 기존 회원 정보를 반환한다
* [ ] 동일 UUID로 중복 회원이 생성되지 않는다
* [ ] UUID 형식 검증을 수행한다
* [ ] 동시 요청 상황에서도 회원은 1건만 생성된다
* [ ] 지원하지 않는 type이면 400 Bad Request를 반환한다

---

## 4. Edge Cases

| 시나리오          | 예상 동작                     |
| ------------- | ------------------------- |
| type 누락       | 400 Bad Request           |
| 지원하지 않는 type  | 400 Bad Request           |
| UUID 누락       | 400 Bad Request           |
| UUID 형식 오류    | 400 Bad Request           |
| 이미 등록된 UUID   | 기존 회원 반환                  |
| 동일 UUID 동시 요청 | 회원 1건만 생성                 |
| DB 저장 실패      | 500 Internal Server Error |

---

## 5. Non-functional Requirements

| 요구사항    | 기준                    |
| ------- | --------------------- |
| 중복 방지   | UUID Unique 제약조건 적용   |
| 데이터 일관성 | 회원 생성 트랜잭션 처리         |
| 확장성     | 추후 소셜 로그인 연동 가능하도록 설계 |
| 로깅      | 회원 생성 및 예외 로그 기록      |

---

## 6. Out of Scope

* 카카오 로그인
* 애플 로그인
* JWT 인증/인가
* 회원 프로필 관리
* 회원 탈퇴
* FCM 토큰 등록

---

## 7. 기술 설계 초안

### Database

#### users

| 컬럼         | 타입                 | 설명        |
| ---------- | ------------------ | --------- |
| id         | bigint PK          | 회원 ID     |
| uuid       | varchar(36) UNIQUE | 디바이스 UUID |
| created_at | timestamp          | 생성 시각     |
| updated_at | timestamp          | 수정 시각     |

### 확장 고려사항

현재는 `users.uuid` 기반으로 회원을 식별한다.

향후 카카오 로그인 등 외부 인증 수단이 추가될 경우:
- `POST /users` + `{"type": "kakao", "token": "..."}` 형태로 확장
- 별도 인증 식별 테이블(`user_authentications`)로 분리할 수 있도록 설계한다.