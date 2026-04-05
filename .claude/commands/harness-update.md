하네스 엔지니어링 인프라를 관리하고 업데이트하는 스킬입니다.

## 관리 대상 파일

| 파일 | 역할 |
|---|---|
| `CLAUDE.md` | AI 에이전트 가이드 |
| `docs/specs/MAP.md` | 아키텍처 맵 |
| `docs/specs/CONVENTIONS.md` | 코딩 컨벤션 |
| `docs/specs/EXECUTION_PLAN.md` | 기능 구현 템플릿 |
| `docs/specs/ADR/*.md` | 아키텍처 결정 기록 |
| `docs/HARNESS_GUIDE.md` | 하네스 구조 가이드 |
| `.editorconfig` | Kotlin 스타일 규칙 |
| `src/test/.../architecture/*.kt` | ArchUnit 아키텍처 테스트 |
| `.githooks/*` | Git hooks |
| `.github/workflows/*.yml` | CI 워크플로우 |

## 절차

### 1단계: 현재 상태 파악

1. 사용자 요청(`$ARGUMENTS`)을 분석하여 어떤 하네스 구성요소를 수정해야 하는지 판단
2. 해당 구성요소의 현재 상태를 읽기

### 2단계: 수정 실행

요청 유형에 따라 분기:

#### ADR 추가
1. `docs/specs/ADR/` 디렉토리에서 마지막 번호 확인
2. 다음 번호로 새 ADR 파일 생성 (한국어)
3. 형식: `{번호}-{주제}.md` (예: `003-redis-cache.md`)
4. 관련된 다른 하네스 파일 업데이트 (CLAUDE.md, MAP.md 등)

#### ktlint 규칙 추가/수정
1. 루트 `.editorconfig` 읽기
2. 요청된 규칙 추가/수정
3. `docs/specs/CONVENTIONS.md`에 관련 컨벤션 반영

#### ArchUnit 규칙 추가
1. 기존 아키텍처 테스트 파일 읽기
2. 새 규칙을 적절한 테스트 파일에 추가 (또는 새 테스트 파일 생성)
3. `docs/specs/MAP.md`에 의존성 규칙 반영

#### 컨벤션 수정
1. `docs/specs/CONVENTIONS.md` 수정
2. `CLAUDE.md`에 요약 반영
3. 필요 시 `.editorconfig`나 ArchUnit 테스트도 함께 업데이트

#### Git Hook / CI 수정
1. 해당 파일 수정
2. `docs/HARNESS_GUIDE.md` 반영

### 3단계: 문서 동기화

수정 사항이 여러 파일에 영향을 미치는 경우, 관련 문서를 모두 동기화하세요:
- `CLAUDE.md` ↔ `docs/specs/CONVENTIONS.md` 일관성
- `docs/specs/MAP.md` ↔ ArchUnit 테스트 일관성
- `docs/HARNESS_GUIDE.md` 업데이트

### 4단계: 검증

```bash
./gradlew harness
```

검증 통과 후 변경 내용을 사용자에게 요약 보고하세요.

## 사용 예시

- `/harness-update 새로운 ADR 추가: Redis 캐시 도입 결정`
- `/harness-update ktlint 줄 길이 규칙 조정`
- `/harness-update ArchUnit에 global 패키지는 도메인 패키지를 의존하면 안 되는 규칙 추가`
- `/harness-update 커밋 메시지 컨벤션에 breaking change 표기법 추가`
