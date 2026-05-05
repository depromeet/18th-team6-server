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
- `service` → 같은 도메인의 `service`, `entity`, `repository`, `dto` + 다른 도메인의 `service` + `global` 사용 가능
- `entity` → 다른 도메인의 `entity`만 참조 가능. 다른 레이어 의존 금지
- `dto` → `entity`를 직접 참조하지 않음. DTO ↔ Entity 변환은 `service` 레이어에서 수행
- `controller`는 다른 도메인의 `controller` 직접 호출 금지

## 코딩 컨벤션

- `val` 우선, 불변 컬렉션 선호
- Entity는 JPA 엔티티 / 도메인 모델
- Service는 `@Service`로 비즈니스 로직 담당
- Controller는 `@RestController`, DTO만 반환 (Entity 직접 노출 금지)
- 네이밍: `*Controller`, `*Service`, `*Repository`, `*Entity` 접미사 사용
- 상세 컨벤션: `docs/specs/CONVENTIONS.md` 참고

## 브랜치 컨벤션

브랜치 형식은 반드시 아래 규칙을 따른다.

- `{작업자}/{목적}/{깃헙이슈번호}-{작업내용}`
- 예: `ziweek/feat/123-add-login`
- 목적: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`
- 이슈 번호는 실제 GitHub issue 번호를 사용한다. `0001` 같은 임의 zero-padding 번호는 금지한다.
- 작업 내용은 짧은 영어 kebab-case로 작성한다.
- 이슈 번호가 없으면 브랜치나 PR을 만들지 말고 먼저 이슈를 확인하거나 생성한다.

## PR-이슈 연동 규칙

- PR 본문에는 브랜치의 이슈 번호와 같은 `Closes #<이슈번호>`를 반드시 작성한다.
- `Fixes #<이슈번호>` 또는 `Resolves #<이슈번호>`도 허용한다.
- 브랜치 이슈 번호와 PR 본문의 이슈 번호가 다르면 CI가 실패한다.

## 커밋 메시지 컨벤션

`{목적}({수정범위}): {수정 내용}` 또는 `{목적}: {수정 내용}`

- 예: `feat(user): 회원가입 API 추가`
- 예: `fix: 필드 누락 수정`
- 목적: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `style`
- 커밋 메시지의 수정 내용은 한국어를 기본으로 작성한다.
- 커밋은 논리적 단위로 나눈다.
- 서로 다른 목적의 변경을 한 커밋에 섞지 않는다.
- 문서 수정, 테스트 추가, 기능 구현, 리팩토링은 가능하면 별도 커밋으로 분리한다.

## 언어 사용 규칙

- 코드 주석, 문서 주석, 리뷰 코멘트, 에이전트 응답, 계획, 리뷰 요약, PR 설명, 커밋 메시지는 한국어를 기본으로 작성한다.
- PR 본문과 리뷰 코멘트의 제목, 요약, 변경 사항, 제안 내용도 한국어로 작성한다.
- 커밋 메시지는 type/scope를 제외한 제목과 본문을 한국어로 작성한다.
- 브랜치 slug, commit type/scope, 패키지명, 클래스명, 명령어 같은 식별자는 영어를 허용한다.

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

## 하네스 가이드

전체 하네스 구조 설명: `docs/HARNESS_GUIDE.md` 참고
