# Obrit Server

Kotlin/Spring Boot 기반 Obrit 서버입니다.

## 로컬 실행

```bash
./gradlew bootRun
```

## 검증

```bash
./gradlew harness
```

## Docker 실행

```bash
docker build -t obrit-server .
docker run -p 8080:8080 obrit-server
```

## API

Swagger UI와 OpenAPI 문서는 서버 실행 후 아래 경로에서 확인할 수 있습니다.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Swagger UI redirect: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

인증이 연동되기 전까지 API 요청에는 아래 헤더로 사용자 ID를 전달합니다. 값은 DB에 존재하는 사용자 ID여야 합니다.

```http
X-User-Id: {userId}
```

로컬 H2(`bootRun`)는 기동 시 빈 DB로 시작합니다. 시드 데이터는 두지 않으므로, Swagger로 호출하기 전에 사용자·아이콘·카테고리 등을 직접 준비하거나 통합 테스트 픽스처 패턴을 참고하세요.

- `GET /categories`
- `POST /categories`
- `DELETE /categories/{categoryId}`
- `GET /items`
- `POST /items`
- `PATCH /items/{itemId}`
- `DELETE /items/{itemId}`
- `POST /items/{itemId}/replacements`
