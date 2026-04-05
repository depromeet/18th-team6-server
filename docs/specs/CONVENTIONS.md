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

`{작업자이름}/{목적}/{이슈번호}-{작업내용}`

| 목적 | 설명 | 예시 |
|---|---|---|
| `feat` | 새 기능 | `ziweek/feat/0001-add-user-api` |
| `fix` | 버그 수정 | `ziweek/fix/0023-fix-null-pointer` |
| `refactor` | 리팩토링 | `ziweek/refactor/0015-simplify-auth` |
| `chore` | 빌드/설정 | `ziweek/chore/0002-update-deps` |
| `docs` | 문서 | `ziweek/docs/0010-add-api-docs` |
| `test` | 테스트 | `ziweek/test/0005-add-unit-tests` |

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
