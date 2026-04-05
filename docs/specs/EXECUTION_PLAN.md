# 기능 구현 실행 계획 템플릿

## 사용법

새로운 기능을 구현할 때 아래 순서를 따른다.
Claude Code에서는 `/dev-plan {기능명}` 스킬을 사용하면 이 순서가 자동 적용된다.

## 구현 순서

### 1. Entity 정의
- 도메인 모델 설계
- JPA 엔티티 클래스 작성
- `src/main/kotlin/.../{도메인}/entity/` 에 배치

### 2. Repository 정의
- 데이터 접근 인터페이스 작성
- `JpaRepository` 상속
- `src/main/kotlin/.../{도메인}/repository/` 에 배치

### 3. Service 구현
- 비즈니스 로직 작성
- DTO ↔ Entity 변환 로직 포함
- `@Transactional` 적용
- `src/main/kotlin/.../{도메인}/service/` 에 배치

### 4. DTO 정의
- 요청 DTO (`*Request`) + 응답 DTO (`*Response`)
- validation 어노테이션 적용
- `companion object`에 `from()` 팩토리 메서드
- `src/main/kotlin/.../{도메인}/dto/` 에 배치

### 5. Controller 구현
- REST API 엔드포인트 작성
- `@RestController` + `@RequestMapping`
- DTO로만 통신
- `src/main/kotlin/.../{도메인}/controller/` 에 배치

### 6. 테스트 작성
- Service 단위 테스트
- Controller 통합 테스트 (MockMvc)
- `src/test/kotlin/.../` 에 배치

### 7. 검증
```bash
./gradlew harness
```
- detekt (정적 분석) 통과
- ArchUnit (아키텍처 규칙) 통과
- 전체 테스트 통과

## 개발 문서 상태 관리

`docs/plans/` 디렉토리에 기능별 개발 문서가 생성된다.

### 상태 아이콘
- 🔴 대기 (pending)
- 🟡 진행중 (in progress)
- 🟢 완료 (done)

### 문서 위치
`docs/plans/YYYY-MM-DD-{기능명}.md`
