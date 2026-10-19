# PRD v2.1 / 02: 카테고리 (욕실·주방 등)

상태: 구현 완료

## 목표

온보딩 목록을 카테고리 탭으로 나눌 수 있게 각 소모품이 속한 카테고리를 내려준다. 소모품 하나는 카테고리 하나에만 속한다.
사용자 소모품은 '사용자' 카테고리에 자동으로 속한다.

PRD의 '카테고리'는 코드에서 `CategoryGroup`이다. 코드의 `Category`는 PRD의 '소모품'이다 (설계 1장 용어 대응).

## 현재 구현

- 카테고리 개념이 없다. `GET /categories`는 프리셋 소모품과 내가 만든 사용자 소모품을 한 목록으로 준다.
- 기본 소모품은 `categories.user_id IS NULL`, 사용자 소모품은 `user_id`가 있다.
- 기본 소모품 데이터는 어드민 백오피스(`Category.updateForAdmin`)나 DB로 관리한다.

## 변경 내용

### 스키마 (Flyway 다음 번호, 예: `V12__add_category_groups.sql`)

```sql
CREATE TABLE category_groups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,        -- PRESET | USER
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (id)
);

ALTER TABLE categories ADD COLUMN category_group_id BIGINT NULL;
CREATE INDEX idx_categories_group ON categories (category_group_id);

-- '사용자' 카테고리는 마이그레이션이 만든다. 탭 순서는 마지막으로 두고 DB에서 바꿀 수 있다.
INSERT INTO category_groups (name, type, display_order, created_at, updated_at)
VALUES ('사용자', 'USER', 1000, NOW(6), NOW(6));

-- 기존 사용자 소모품을 '사용자' 카테고리로 옮긴다.
UPDATE categories SET category_group_id = (SELECT id FROM category_groups WHERE type = 'USER')
WHERE user_id IS NOT NULL;
```

- FK 제약은 두지 않는다. 기존 `categories`와 `items`도 FK 없이 ID 컬럼으로 참조한다.
- 기본 소모품의 카테고리 배정과 욕실·주방 같은 `PRESET` 카테고리 행은 사용자가 운영 DB에 직접 넣는다. 그 전까지 기본 소모품의 카테고리는 비어 있다.

### 엔티티

- `category/entity/CategoryGroup.kt`: `id`, `name`, `type: CategoryGroupType`, `displayOrder`, `BaseTimeEntity` 상속.
- `category/entity/CategoryGroupType.kt`: `PRESET`, `USER`.
- `Category.categoryGroupId: Long?` 추가.

### 사용자 소모품 생성 시 자동 배정

- `CategoryService.createCategory`와 `getOrCreateUserCategoryId`가 `type = USER`인 카테고리 id를 넣는다.
- `USER` 카테고리 행이 없으면 등록을 막지 않고 `null`로 저장하고 경고 로그를 남긴다.

### API

- `GET /categories/groups` 신규: 삭제되지 않은 카테고리를 `displayOrder` 오름차순으로 준다.

  ```json
  [{ "categoryGroupId": 1, "name": "욕실", "type": "PRESET", "displayOrder": 1 },
   { "categoryGroupId": 9, "name": "사용자", "type": "USER", "displayOrder": 1000 }]
  ```

- `GET /categories` 응답 `CategoryResponse`에 `categoryGroupId: Long?`를 추가한다. 나머지 필드와 정렬은 유지한다.
- 탭 구성(빈 탭 숨김, '사용자' 탭 노출 조건)은 앱이 정한다. 서버는 카테고리 목록과 소속만 준다.

## 파일과 설정

| 파일 | 변경 |
| --- | --- |
| `src/main/resources/db/migration/V{다음}__add_category_groups.sql` | 신규 |
| `category/entity/CategoryGroup.kt`, `CategoryGroupType.kt` | 신규 |
| `category/entity/Category.kt` | `categoryGroupId` |
| `category/repository/CategoryGroupRepository.kt` | 신규 |
| `category/service/CategoryService.kt`, `CategoryQueryService.kt` | 자동 배정, 목록 |
| `category/controller/CategoryController.kt`, `docs/CategoryControllerApi.kt` | `GET /categories/groups` |
| `category/dto/response/` | `CategoryGroupResponse` 신규, `CategoryResponse` 필드 추가 |
| `src/main/resources/data.sql` | `category_groups` 시드(욕실·주방·사용자)와 `categories.category_group_id`. INSERT 순서 `users → icons → category_groups → categories` |
| `CLAUDE.md` | `data.sql` INSERT 순서 문구 갱신 |

## 운영 반영 순서

1. 앱 배포로 마이그레이션 적용 (`category_groups` 생성, '사용자' 카테고리 생성, 기존 사용자 소모품 이동)
2. 사용자가 기획 표를 받아 `PRESET` 카테고리 행과 기본 소모품의 `category_group_id`를 운영 DB에 입력
3. 앱 v2.1이 탭 화면을 연다

2번 전에 앱 v2.1이 나가면 기본 소모품이 어느 탭에도 속하지 않는다. 출시 일정을 맞춘다.

## 완료 기준

- 운영과 같은 MySQL 버전의 로컬 컨테이너에서 V1부터 새 마이그레이션까지 적용되고, 기존 사용자 소모품이 '사용자' 카테고리로 옮겨진다. (dev·test 프로필은 Flyway가 꺼져 있어 자동 테스트로 확인되지 않는다.)
- `GET /categories/groups`가 순서대로 카테고리를 준다.
- `GET /categories`의 각 소모품에 `categoryGroupId`가 있다. 사용자 소모품은 '사용자' 카테고리 id다.
- 두 생성 경로로 만든 사용자 소모품이 '사용자' 카테고리에 속한다.
- 로컬 기동 시 `data.sql`이 오류 없이 들어간다. `./gradlew harness` 통과.
