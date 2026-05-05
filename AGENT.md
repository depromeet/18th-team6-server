# AI Agent Instructions

이 프로젝트에서 작업하기 전에 반드시 아래 파일을 먼저 읽으세요.

## 필수 참조

1. **`CLAUDE.md`** (프로젝트 루트) - 반드시 최우선으로 읽을 것
   - 프로젝트 개요 및 기술 스택
   - DDD 패키지 구조 및 의존성 규칙
   - 코딩 컨벤션
   - 브랜치 / 커밋 메시지 컨벤션
   - 빌드 및 검증 커맨드
   - 사용 가능한 스킬 (`/dev-plan`, `/review`, `/harness-update`)

## 추가 참조

- `docs/specs/MAP.md` - 아키텍처 맵 (패키지 구조, 요청 흐름, 의존성 방향)
- `docs/specs/CONVENTIONS.md` - 상세 코딩 컨벤션 (코드 예제 포함)
- `docs/specs/EXECUTION_PLAN.md` - 기능 구현 순서 템플릿
- `docs/specs/ADR/` - 아키텍처 결정 기록
- `docs/HARNESS_GUIDE.md` - 하네스 엔지니어링 전체 구조 가이드

## 작업 전 체크리스트

- [ ] `CLAUDE.md`를 읽었는가?
- [ ] 작업할 도메인의 패키지 구조를 이해했는가?
- [ ] 의존성 규칙을 숙지했는가? (controller → 같은 도메인 controller/service/dto + global, DTO ↔ Entity 변환은 service 레이어 소유 등)
- [ ] 커밋 전 `./gradlew harness` 실행 방법을 알고 있는가?
