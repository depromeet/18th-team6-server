# Obrit - Project Guide

## 프로젝트 개요

- **이름**: Obrit
- **스택**: Spring Boot 4.0.5, Kotlin 2.2.21, Java 17, Gradle
- **패키지**: `depromeet.hotsix.obrit`

## 아키텍처 (DDD 패키지 구조)

도메인별로 패키지를 묶고, 각 도메인 안에 레이어를 둔다.

```
depromeet.hotsix.obrit/
  {도메인}/              # 예: user, post
    controller/          # @RestController - REST API 엔드포인트
    service/             # @Service - 비즈니스 로직
    dto/                 # 요청/응답 DTO
    entity/              # JPA 엔티티 / 도메인 모델
    repository/          # 데이터 접근 인터페이스 및 구현
  global/                # 공통 모듈
    config/              # 설정 클래스
    exception/           # 예외 처리
    common/              # 공통 유틸리티
```

### 의존성 규칙

- `controller` → 같은 도메인의 `controller`, `service`, `dto` + `global`만 사용
- `service` → 같은 도메인의 `service`, `entity`, `repository`, `dto`
  + 다른 도메인의 `service`
  + 다른 도메인 entity의 enum/value object
  + `global` 사용 가능
- `entity` → 다른 도메인의 `entity`만 참조 가능. 다른 레이어 의존 금지
- `dto` → JPA Entity 및 식별자를 가진 도메인 객체는 직접 참조하지 않음.
  enum/value object는 참조 가능. DTO ↔ Entity 변환은 `service` 레이어에서 수행
- `controller`는 다른 도메인의 `controller` 직접 호출 금지

> **enum/value object 정의**: Kotlin `enum class`와 `value class`(inline class)만을 가리킨다.
> 일반 `data class`는 식별자 유무와 무관하게 포함되지 않는다.

## 코딩 컨벤션

- `val` 우선, 불변 컬렉션 선호
- Entity는 JPA 엔티티 / 도메인 모델
- Service는 `@Service`로 비즈니스 로직 담당
- Controller는 `@RestController`, DTO만 반환 (Entity 직접 노출 금지)
- 네이밍: `*Controller`, `*Service`, `*Repository`, `*Entity` 접미사 사용
- 상세 컨벤션: `docs/specs/CONVENTIONS.md` 참고

## 브랜치 컨벤션

`{작업자이름}/{목적}/{이슈번호}-{작업내용}`

- 예: `ziweek/feat/0001-add-schema`
- 목적: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`

## 커밋 메시지 컨벤션

`{목적}({수정범위}): {수정 내용}` 또는 `{목적}: {수정 내용}`

- 예: `feat(user): 회원가입 API 추가`
- 예: `fix: 필드 누락 수정`
- 목적: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `style`

## 빌드 및 검증

```bash
./gradlew installGitHooks # 로컬 git hook 설치 (초기 셋업 1회)
./gradlew build      # 빌드 및 전체 검증
./gradlew test       # 테스트 실행
./gradlew ktlintCheck # 스타일 검증
./gradlew ktlintFormat # 스타일 자동 수정
./gradlew harness    # 전체 검증 (ktlint + ArchUnit + 테스트)
```

커밋 전에 반드시 `./gradlew harness` 실행.

## 스킬 (슬래시 커맨드)

- `/dev-plan {기능명}` - 개발 문서 작성 → Task 분해 → 순차 실행 → 상태 추적
- `/review` - 워크스페이스 diff를 코드 리뷰하고 코멘트 남기기 (Approve는 사람이 담당)
- `/harness-update {작업}` - 하네스 인프라 관리/업데이트 (ADR, ktlint, ArchUnit 등)
- `/organize-domain-model` - 도메인 enum/value class 배치 + 상태 판단 책임 위치 가이드 (DTO에 enum, Service에 판단 로직이 쌓일 때)

## 하네스 가이드

전체 하네스 구조 설명: `docs/HARNESS_GUIDE.md` 참고
