# 하네스 엔지니어링 가이드

## 하네스 엔지니어링이란?

하네스 엔지니어링은 AI 코딩 에이전트가 코드베이스에서 효과적이고 안전하게 작업할 수 있도록 **인프라를 구축하는 방법론**이다.

핵심 공식: **Agent = Model + Harness**

모델(LLM)이 아무리 뛰어나도 하네스 없이는 잘못된 위치에 코드를 생성하거나 아키텍처 규칙을 위반할 수 있다. 하네스는 이를 방지하는 두 가지 메커니즘으로 구성된다:

- **Feedforward (가이드)**: 코드 생성 **전에** 에이전트를 올바른 방향으로 안내
- **Feedback (검증)**: 코드 생성 **후에** 문제를 감지하고 수정을 유도

## 이 프로젝트의 하네스 구조

```
┌─────────────────────────────────────────────────┐
│                  FEEDFORWARD (가이드)              │
│                                                   │
│  claude.md ─────── AI 에이전트 자동 가이드         │
│  docs/specs/                                      │
│    MAP.md ──────── 아키텍처 맵                     │
│    CONVENTIONS.md  코딩 컨벤션                     │
│    EXECUTION_PLAN  구현 순서 템플릿                │
│    ADR/ ────────── 아키텍처 결정 기록              │
│  .claude/commands/                                │
│    dev-plan.md ─── /dev-plan 스킬                  │
│    review.md ───── /review 스킬                    │
│    harness-update  /harness-update 스킬            │
│                                                   │
├─────────────────────────────────────────────────┤
│                  FEEDBACK (검증)                   │
│                                                   │
│  ArchUnit ──────── 아키텍처 규칙 자동 테스트       │
│  ktlint ────────── Kotlin 스타일 린트              │
│  Git Hooks                                        │
│    pre-commit ──── 커밋 시 ktlintCheck             │
│    pre-push ────── 푸시 시 전체 harness             │
│  GitHub Actions                                   │
│    harness.yml ─── PR 시 CI 검증                   │
│    pr-issue-link.yml ─ PR-이슈 연결 검증           │
│                                                   │
└─────────────────────────────────────────────────┘
```

## Feedforward 구성요소

### claude.md
- **위치**: 프로젝트 루트
- **역할**: Claude Code가 자동으로 읽는 가이드 파일
- **내용**: 프로젝트 개요, DDD 패키지 구조, 코딩 컨벤션, 브랜치/커밋/PR-이슈 연동 규칙, 한국어 기본 사용 규칙, 빌드 커맨드

### docs/specs/MAP.md
- **역할**: 아키텍처 전체 맵
- **내용**: 패키지 구조, 요청 흐름도, 레이어별 책임, 의존성 방향

### docs/specs/CONVENTIONS.md
- **역할**: 상세 코딩 컨벤션
- **내용**: 네이밍 규칙, 각 레이어별 코드 예제, 브랜치/커밋/PR/주석 언어 컨벤션

### docs/specs/EXECUTION_PLAN.md
- **역할**: 기능 구현 순서 템플릿
- **내용**: Entity → Repository → DTO → Service → Controller → Test → Harness 검증

### docs/specs/ADR/
- **역할**: 아키텍처 결정 기록 (Architecture Decision Records)
- **파일**: 번호 순서로 관리 (001, 002, ...)
- **수정**: `/harness-update` 스킬로 추가

## Feedback 구성요소

### ArchUnit 테스트
- **위치**: `src/test/.../architecture/`
- **역할**: 아키텍처 규칙을 JUnit 테스트로 강제
- **파일**:
  - `LayerDependencyTest.kt` - 레이어 간 의존성 방향 검증
  - `NamingConventionTest.kt` - 네이밍 컨벤션 검증

### ktlint
- **설정**: 루트 `.editorconfig`
- **역할**: Kotlin 스타일 린트와 포맷 검증 (최대 줄 길이, 와일드카드 임포트 방지 등)
- **명령**:
  - `./gradlew ktlintCheck`
  - `./gradlew ktlintFormat`

### Git Hooks
- **위치**: `.githooks/`
- **설치**: 초기 셋업 시 `./gradlew installGitHooks` 1회 실행
- **pre-commit**: `ktlintCheck` 실행 (~5초). 실패 시 커밋 차단
- **pre-push**: `./gradlew harness` 전체 실행 (~30초~1분). 실패 시 푸시 차단

### GitHub Actions CI
- **위치**: `.github/workflows/harness.yml`
- **트리거**: main 브랜치 대상 PR
- **역할**: 로컬 hook을 우회하더라도 CI에서 반드시 잡히도록 이중 검증

### PR-이슈 연동 검증
- **위치**: `.github/workflows/pr-issue-link.yml`
- **트리거**: PR 생성, 수정, 재오픈, 동기화, ready-for-review
- **역할**: 브랜치명과 PR 본문이 같은 GitHub 이슈 번호를 참조하도록 강제
- **검증 규칙**:
  - 브랜치명은 `{작업자}/{목적}/{깃헙이슈번호}-{작업내용}` 형식이어야 한다
  - PR 본문은 `Closes #<이슈번호>`, `Fixes #<이슈번호>`, `Resolves #<이슈번호>` 중 하나를 포함해야 한다
  - 브랜치 이슈 번호와 PR 본문의 이슈 번호가 다르면 실패한다

### 한국어 기본 사용 규칙
- **위치**: `claude.md`, `docs/specs/CONVENTIONS.md`, `.github/PULL_REQUEST_TEMPLATE.md`, `.claude/commands/review.md`
- **역할**: 주석, PR 설명, 리뷰 코멘트, 커밋 메시지, 에이전트 응답의 기본 언어를 한국어로 고정
- **허용 예외**: 브랜치 slug, commit type/scope, 패키지명, 클래스명, 명령어 같은 식별자는 영어 사용 가능

## 검증 흐름도

```
[개발자/에이전트 코딩]
    ↓
[git commit] → pre-commit hook → ktlintCheck
    │                                 ↓ 실패 시 커밋 차단
    ↓
[git push] → pre-push hook → ./gradlew harness
    │                              ↓ 실패 시 푸시 차단
    ↓
[PR 생성] → pr-issue-link.yml → 브랜치명/PR 본문 이슈 번호 검증
    │                              ↓ 실패 시 머지 차단
    │       → /review 스킬 → 코드 리뷰 코멘트
    │       → harness.yml CI → ./gradlew build
    │                              ↓ 실패 시 머지 차단
    ↓
[사람이 Approve] → [PR 머지] → main에 반영
```

## 스킬 사용법

### /dev-plan {기능명}
기능 개발 워크플로우를 자동화합니다.

```
/dev-plan 회원가입 기능
```

1. `docs/plans/` 에 개발 문서 생성
2. Task를 DDD 순서로 분해
3. 하나씩 실행하며 상태 추적 (🔴→🟡→🟢)
4. 마지막에 `./gradlew harness` 검증

### /review
현재 변경사항을 코드 리뷰합니다.

```
/review
```

1. 워크스페이스 diff 분석
2. 아키텍처/컨벤션/보안 관점으로 리뷰
3. 문제 라인에 코멘트 ([필수]/[권장]/[참고])
4. Approve는 사람이 담당

### /harness-update {작업 내용}
하네스 인프라 자체를 관리합니다.

```
/harness-update 새로운 ADR 추가: Redis 캐시 도입
/harness-update ktlint 규칙/설정 조정
```

1. 관련 하네스 파일 수정
2. 연관 문서 동기화
3. `./gradlew installGitHooks`, `./gradlew build`, `./gradlew harness` 기준으로 영향 점검

## 하네스 수정/확장 방법

### 새로운 아키텍처 규칙 추가
1. `/harness-update` 스킬 사용 또는 수동으로:
2. `src/test/.../architecture/` 에 ArchUnit 테스트 추가
3. `docs/specs/MAP.md` 에 규칙 문서화
4. `claude.md` 에 요약 반영

### 새로운 스타일 규칙 추가
1. 루트 `.editorconfig` 에 `ktlint` 관련 설정 추가
2. `docs/specs/CONVENTIONS.md` 에 관련 컨벤션 문서화

### 새로운 ADR 추가
1. `docs/specs/ADR/{번호}-{주제}.md` 파일 생성
2. 상태/맥락/결정/이유/결과 섹션 포함

### 새로운 스킬 추가
1. `.claude/commands/{스킬명}.md` 파일 생성
2. 이 가이드 문서에 사용법 추가

## 참고 자료

- [Anthropic - Harness Design for Long-Running Apps](https://www.anthropic.com/engineering/harness-design-long-running-apps)
- [Martin Fowler - Harness Engineering for Coding Agents](https://martinfowler.com/articles/exploring-gen-ai/harness-engineering.html)
- [OpenAI - Harness Engineering](https://openai.com/index/harness-engineering/)
