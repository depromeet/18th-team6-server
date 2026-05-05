# 아키텍처 맵

## 개요

Obrit은 DDD(Domain-Driven Design) 기반 패키지 구조를 사용한다.
각 도메인은 독립적인 패키지로 분리되며, 내부에 레이어를 둔다.

## 패키지 구조

```
depromeet.hotsix.obrit/
  {도메인}/
    controller/     # Presentation 레이어
    service/        # Application 레이어
    dto/            # Data Transfer Objects
    entity/         # Domain 레이어
    repository/     # Data Access 레이어
  global/
    config/         # 앱 설정 (@Configuration)
    exception/      # 전역 예외 처리 (@ControllerAdvice)
    common/         # 공통 유틸리티, 상수, Base Entity 등
```

## 요청 흐름

```
HTTP Request
    ↓
Controller (dto로 요청 수신)
    ↓
Service (비즈니스 로직 처리, dto ↔ entity 변환)
    ↓
Repository (데이터 접근)
    ↓
Database
    ↓
Repository → Service (entity 반환)
    ↓
Service → Controller (dto로 변환하여 반환)
    ↓
HTTP Response
```

## 레이어별 책임

### Controller
- HTTP 요청/응답 처리
- 요청 유효성 검증 (`@Valid`)
- Service 호출 및 DTO 반환
- 비즈니스 로직 포함 금지
- 같은 도메인의 `controller`, `service`, `dto`와 `global` 패키지만 참조

### Service
- 비즈니스 로직 수행
- 트랜잭션 관리 (`@Transactional`)
- DTO ↔ Entity 변환
- 다른 도메인의 Service 호출 가능
- 필요 시 `service` 패키지 내부 mapper/assembler로 변환 로직 분리 가능

### Entity
- 도메인 모델 정의
- JPA 엔티티 매핑
- 도메인 규칙/행위 포함 가능
- 외부 레이어 의존 금지

### Repository
- 데이터 접근 인터페이스 (Spring Data JPA)
- 커스텀 쿼리 정의

### DTO
- 외부 인터페이스용 데이터 구조
- 요청 DTO (Request), 응답 DTO (Response) 분리
- Entity를 직접 노출하거나 참조하지 않음

## 의존성 방향

```
Controller → Service → Repository
    ↓           ↓
   DTO     Entity, DTO
```

- 의존성은 항상 **안쪽(도메인)** 방향으로 흐른다
- Entity는 어떤 레이어에도 의존하지 않는다
- Controller는 같은 도메인의 `controller/service/dto`와 `global`만 참조한다
- Service는 같은 도메인의 `service/entity/repository/dto`, 다른 도메인의 `service`, `global`만 참조한다
- DTO ↔ Entity 변환은 Service 레이어에서 수행한다
- Controller는 다른 도메인의 Controller를 직접 호출하지 않는다
- 도메인 간 통신은 Service를 통해서만 한다
