## 1. 설계 원칙

- 관측성 코드는 비즈니스 로직과 분리한다.
- 관측성 코드도 테스트 가능하게 하게하기!

---

## 2. 수집 방식

세 갈래로 나눠서 처리한다.

| 방식 | 담당 |
| --- | --- |
| Micrometer 자동 | HTTP RED, JVM, HikariCP, Redis |
| Filter | MDC requestId |
| AOP 어노테이션 | 비즈니스 메트릭 |

### 1) Micrometer 자동 수집

actuator와 prometheus 의존성 추가 후 application.yml에서 prometheus 엔드포인트만 열어주면 HTTP RED, JVM, HikariCP, Redis 메트릭이 자동 수집된다.

### 2) Filter

Filter와 Interceptor 중 Filter를 쓴다. 인증 실패처럼 Spring 초기 단계에서 발생하는 에러 로그에도 requestId가 찍혀야 추적이 되기 때문.

### 3) AOP

Micrometer 기본 태그가 존재하지만 세부 비즈니스 정보를 담는건 불가능하다.

따라서 커스텀 어노테이션과 Aspect를 만들엇 비즈니스 지표 수집하도록 설계

---

## 3. MDC Filter 구현

요청마다 requestId를 생성해서 MDC에 저장한다.

예시 코드)

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String MDC_KEY = "requestId";
    private static final String HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(
        HttpServletRequest req, HttpServletResponse res, FilterChain chain
    ) throws ServletException, IOException {

        String requestId = Optional.ofNullable(req.getHeader(HEADER))
            .filter(s -> !s.isBlank())
            .orElse(UUID.randomUUID().toString());

        MDC.put(MDC_KEY, requestId);
        res.setHeader(HEADER, requestId);

        try {
            chain.doFilter(req, res);
        } finally {
            MDC.clear();
        }
    }
}
```

주의할 점:

- `@Order(HIGHEST_PRECEDENCE)`로 다른 Filter보다 먼저 실행시켜야 모든 로그에 requestId가 남는다.
- `MDC.clear()`는 스레드 풀 재사용 시 leak 방지용. 빼먹으면 다음 요청에 이전 값이 남는다.
- 외부에서 온 헤더가 있으면 재사용, 없으면 새로 생성.
- 응답에도 내려주면 클라이언트가 문제 제보 시 ID를 같이 줄 수 있다.

---

## 4. AOP

커스텀 어노테이션으로 메서드 인자를 태그로 추출한다.

예시 코드)

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackBusinessMetric {
    String value();
    String[] tagFrom() default {};  
}
```

```java
@Aspect
@Component
@RequiredArgsConstructor
public class BusinessMetricAspect {

    private final MeterRegistry registry;

    @Around("@annotation(annotation)")
    public Object track(ProceedingJoinPoint pjp, TrackBusinessMetric annotation) throws Throwable {
        try {
            Object result = pjp.proceed();
            increment(annotation, pjp, "success");
            return result;
        } catch (Exception e) {
            increment(annotation, pjp, "failure");
            throw e;
        }
    }

    private void increment(TrackBusinessMetric ann, ProceedingJoinPoint pjp, String outcome) {
        Counter.builder(ann.value())
            .tags(extractTags(pjp, ann.tagFrom()))
            .tag("outcome", outcome)
            .register(registry)
            .increment();
    }
}
```

성공과 실패를 모두 카운트하고 예외는 다시 던져서 비즈니스 로직에 영향이 가지 않게 한다.

예시 코드)

```java
@TrackBusinessMetric(value = "product_registered_total", tagFrom = {"request.category"})
public Product register(ProductCreateRequest request) {
    return productRepository.save(...);
}
```

---

## 패키지 구조

```
src/main/java/com/your/project/
├── domain/
│   └── product/
│       └── ProductService.java          // 관측 모름
│
└── observability/
    ├── filter/
    │   └── RequestIdFilter.java
    ├── annotation/
    │   └── TrackBusinessMetric.java
    └── aspect/
        └── BusinessMetricAspect.java
```