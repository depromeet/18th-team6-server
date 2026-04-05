현재 워크스페이스의 변경사항을 코드 리뷰하고 코멘트를 남기는 스킬입니다.
Approve는 사람이 합니다. 이 스킬은 리뷰 코멘트만 담당합니다.

## 절차

### 1단계: 리뷰 준비

1. `docs/specs/CONVENTIONS.md`를 읽어 코딩 컨벤션을 파악하세요
2. `docs/specs/MAP.md`를 읽어 아키텍처 규칙을 파악하세요
3. `GetWorkspaceDiff`로 전체 변경사항 통계를 확인하세요 (stat: true)

### 2단계: 파일별 리뷰

변경된 각 파일에 대해 `GetWorkspaceDiff`로 상세 diff를 확인하고 아래 관점으로 리뷰하세요.

#### 리뷰 관점

**아키텍처 (필수)**
- DDD 패키지 구조를 따르는가? (controller/service/dto/entity/repository)
- 레이어 의존성 방향이 올바른가?
  - controller → 같은 도메인의 controller, service, dto + global만 의존
  - service → 같은 도메인의 service, entity, repository, dto + 다른 도메인 service + global만 의존
  - entity → 외부 레이어 의존 금지
- Controller에서 Entity를 직접 반환하지 않는가?
- DTO ↔ Entity 변환이 Service에서 수행되는가?

**코딩 컨벤션 (필수)**
- 네이밍 규칙: *Controller, *Service, *Repository 접미사
- `val` 우선, 불변 컬렉션 사용
- 함수 30줄 이내, 파라미터 5개 이내
- 와일드카드 임포트 미사용

**테스트 (권장)**
- 새로운 Service에 단위 테스트가 있는가?
- 새로운 Controller에 통합 테스트가 있는가?

**보안 (필수)**
- SQL 인젝션 취약점
- XSS 취약점
- 인증/인가 누락
- 민감 정보 노출 (비밀번호, 토큰 등)

### 3단계: 코멘트 작성

`DiffComment`를 사용하여 문제가 있는 라인에 직접 코멘트를 남기세요.

#### 코멘트 형식

```
[심각도] 설명

제안: 수정 방법
```

#### 심각도 기준
- `[필수]` - 머지 전 반드시 수정 필요 (아키텍처 위반, 보안 문제, 버그)
- `[권장]` - 수정하면 좋지만 필수는 아님 (컨벤션 경미한 위반, 가독성)
- `[참고]` - 참고 사항, 개선 아이디어

### 4단계: 리뷰 요약

모든 코멘트를 남긴 후, 리뷰 결과를 요약하여 사용자에게 전달하세요:
- 총 코멘트 수 (필수/권장/참고 별)
- 주요 이슈 목록
- 전반적인 코드 품질 평가
