# 코딩 컨벤션

## 일반 규칙

- `val` 우선, `var` 사용은 최소화
- 불변 컬렉션 (`List`, `Set`, `Map`) 우선 사용
- 매직 넘버/문자열은 상수로 추출
- 함수는 30줄 이내, 파라미터는 5개 이내
- 와일드카드 임포트(`*`) 금지
- 한 줄 최대 120자

## 네이밍 규칙

| 대상 | 규칙 | 예시 |
|---|---|---|
| Controller | `*Controller` | `UserController` |
| Service | `*Service` | `UserService` |
| Repository | `*Repository` | `UserRepository` |
| Entity | `*Entity` 또는 도메인 이름 | `User`, `UserEntity` |
| 요청 DTO | `*Request` | `CreateUserRequest` |
| 응답 DTO | `*Response` | `UserResponse` |
| 예외 | `*Exception` | `UserNotFoundException` |

## Controller 컨벤션

```kotlin
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
) {

    @PostMapping
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        val response = userService.createUser(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
```

- `@RestController` + `@RequestMapping` 사용
- 생성자 주입 (trailing comma 사용)
- DTO만 반환, Entity 직접 노출 금지
- `ResponseEntity`로 감싸서 반환

## Service 컨벤션

```kotlin
@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
) {

    @Transactional
    fun createUser(request: CreateUserRequest): UserResponse {
        val user = User(
            name = request.name,
            email = request.email,
        )
        val savedUser = userRepository.save(user)
        return UserResponse(
            id = savedUser.id,
            name = savedUser.name,
            email = savedUser.email,
        )
    }
}
```

- 클래스 레벨에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional`
- DTO ↔ Entity 변환은 Service 레이어에서 수행
- 필요 시 `service` 패키지 내부 mapper/assembler로 변환 로직을 분리할 수 있음

## Entity 컨벤션

```kotlin
@Entity
@Table(name = "users")
class User(
    @Column(nullable = false)
    val name: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
)
```

- `data class` 사용 금지 (JPA 프록시 호환 문제)
- Spring 어노테이션은 JPA 관련만 허용 (`@Entity`, `@Table`, `@Column` 등)
- ID는 마지막 파라미터, 기본값 0

## DTO 컨벤션

```kotlin
data class CreateUserRequest(
    @field:NotBlank
    val name: String,

    @field:Email
    val email: String,
)

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
)
```

- `data class` 사용
- 요청 DTO: validation 어노테이션은 `@field:` prefix 사용
- DTO는 Entity 타입을 직접 참조하지 않음

## Repository 컨벤션

```kotlin
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
}
```

- `JpaRepository` 상속
- Spring Data JPA 쿼리 메서드 활용
- 복잡한 쿼리는 `@Query` 또는 QueryDSL 사용

## 테스트 컨벤션

- 단위 테스트: `{ClassName}Test.kt`
- 통합 테스트: `{ClassName}IntegrationTest.kt`
- 테스트 메서드 이름: 백틱 사용하여 한국어 허용
  ```kotlin
  @Test
  fun `사용자를 생성하면 ID가 반환된다`() { ... }
  ```

## 브랜치 컨벤션

브랜치 형식은 반드시 아래 규칙을 따른다.

`{작업자}/{목적}/{깃헙이슈번호}-{작업내용}`

- 이슈 번호는 실제 GitHub issue 번호를 사용한다.
- `0001` 같은 임의 zero-padding 번호는 금지한다.
- 작업 내용은 짧은 영어 kebab-case로 작성한다.
- 이슈 번호가 없으면 브랜치나 PR을 만들지 말고 먼저 이슈를 확인하거나 생성한다.

| 목적 | 설명 | 예시 |
|---|---|---|
| `feat` | 새 기능 | `ziweek/feat/123-add-user-api` |
| `fix` | 버그 수정 | `ziweek/fix/23-fix-null-pointer` |
| `refactor` | 리팩토링 | `ziweek/refactor/15-simplify-auth` |
| `chore` | 빌드/설정 | `ziweek/chore/2-update-deps` |
| `docs` | 문서 | `ziweek/docs/10-add-api-docs` |
| `test` | 테스트 | `ziweek/test/5-add-unit-tests` |

## PR-이슈 연동 컨벤션

- PR 본문에는 브랜치의 이슈 번호와 같은 `Closes #<이슈번호>`를 반드시 작성한다.
- `Fixes #<이슈번호>` 또는 `Resolves #<이슈번호>`도 허용한다.
- 브랜치 이슈 번호와 PR 본문의 이슈 번호가 다르면 CI가 실패한다.
- PR 설명, 리뷰 요청, 리뷰 코멘트, 리뷰 요약은 한국어를 기본으로 작성한다.

## 커밋 메시지 컨벤션

`{목적}({범위}): {내용}` 또는 `{목적}: {내용}`

```
feat(user): 회원가입 API 추가
fix(auth): 토큰 만료 검증 누락 수정
refactor: 공통 예외 처리 로직 분리
docs: API 문서 업데이트
test(user): 회원가입 서비스 단위 테스트 추가
chore: Gradle 의존성 업데이트
```

- 커밋 메시지의 내용은 한국어를 기본으로 작성한다.
- 커밋은 논리적 단위로 나눈다.
- 서로 다른 목적의 변경을 한 커밋에 섞지 않는다.
- 문서 수정, 테스트 추가, 기능 구현, 리팩토링은 가능하면 별도 커밋으로 분리한다.

## 언어 사용 컨벤션

- 코드 주석, 문서 주석, 리뷰 코멘트, 에이전트 응답, 계획, 리뷰 요약, PR 설명, 커밋 메시지는 한국어를 기본으로 작성한다.
- PR 본문과 리뷰 코멘트의 제목, 요약, 변경 사항, 제안 내용도 한국어로 작성한다.
- 커밋 메시지는 type/scope를 제외한 제목과 본문을 한국어로 작성한다.
- 브랜치 slug, commit type/scope, 패키지명, 클래스명, 명령어 같은 식별자는 영어를 허용한다.
