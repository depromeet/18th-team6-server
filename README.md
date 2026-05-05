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

개발용 요청에는 아래 헤더를 사용합니다.

```http
X-User-Id: 1
```

- `GET /categories`
- `POST /categories`
- `DELETE /categories/{categoryId}`
- `GET /items`
- `POST /items`
- `PATCH /items/{itemId}`
- `DELETE /items/{itemId}`
- `POST /items/{itemId}/replacements`
