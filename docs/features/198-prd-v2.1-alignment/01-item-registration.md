# PRD v2.1 / 01: 소모품 등록·편집

상태: 설계 초안, 구현 전

## 목표

모든 등록 경로(직접 추가, 온보딩 일괄 등록, 영수증)에서 등록일을 마지막 교체일로 쓴다. 상세 편집에서 마지막 교체일을 바꿀 수 없게 한다.
사용자 소모품은 공통 기본 주기 90일과 공용 아이콘으로 만든다.

## 현재 구현

- `CreateItemRequest.lastReplacementPeriod`(`WITHIN_WEEK` 등 4종)를 `LastReplacementPeriod.toDate`로 4·21·45·90일 전 날짜로 바꿔 저장한다. 미선택이면 오늘.
- `UpdateItemRequest.lastReplacedDate`로 마지막 교체일을 바꿀 수 있고, 바꾸면 다음 교체일을 다시 계산하고 지연 알림 상태를 초기화한다 (`Item.update`).
- 사용자 소모품을 만드는 경로는 두 가지다.
  - `POST /categories`: `iconId` 필수, 주기는 지정하지 않아 엔티티 기본값 1일.
  - `POST /items`, `POST /items/bulk`의 `newCategoryName`: `newCategoryDefaultReplacementIntervalDays ?: 1`로 `CategoryService.getOrCreateUserCategoryId` 호출. 같은 이름의 사용자 소모품이 있으면 재사용한다.
- 영수증 분석(`ReceiptService.buildAnalyzedItems`)은 `AnalyzedItem.suggestedReplacementIntervalDays`에 OCR 추천값(없으면 1)을 넣는다. 클라이언트는 매칭 실패 소모품에 이 값을 `newCategoryDefaultReplacementIntervalDays`로 넘긴다.

## 변경 내용

### 마지막 교체일

- `LastReplacementPeriod` enum과 `CreateItemRequest.lastReplacementPeriod`를 삭제한다. `ItemService.saveItem`은 항상 `LocalDate.now(clock)`을 마지막 교체일로 쓴다.
- `UpdateItemRequest.lastReplacedDate`를 삭제하고 `Item.update`에서 해당 인자와 분기를 뺀다. 교체 주기 변경 시 다음 교체일 재계산과 지연 알림 초기화는 유지한다.
- 마지막 교체일은 `POST /items/{id}/replacements`(교체 등록)로만 바뀐다. 교체 등록의 `replacedDate` 입력은 이번 범위에서 바꾸지 않는다.

### 사용자 소모품

- 기본 주기 상수 90일을 `Category`에 둔다 (예: `Category.USER_DEFAULT_REPLACEMENT_INTERVAL_DAYS`).
- `POST /categories`: 주기 90일, 아이콘은 공용 아이콘(id 1, 현재 `DEFAULT_CATEGORY_ICON_ID`)으로 고정한다. `CreateCategoryRequest.iconId`는 선택값으로 바꾸고 값이 와도 쓰지 않는다.
- `CreateItemRequest.newCategoryDefaultReplacementIntervalDays`를 삭제한다. `newCategoryName`으로 새로 만들 때 주기는 항상 90일이다.
- 같은 이름의 기존 사용자 소모품을 재사용할 때는 그 소모품의 주기를 그대로 쓴다 (현행 유지).
- `CreateItemRequest.replacementIntervalDays`(내 소모품의 주기 직접 지정)는 유지한다.
- 공용 아이콘 id 상수가 `CategoryService`와 `CategoryQueryService`에 중복되어 있다. 이번에 손대는 쪽에서 한곳으로 모은다.

### 영수증

- 매칭 실패 소모품의 `suggestedReplacementIntervalDays`는 90을 내려준다. 매칭된 소모품은 그 소모품의 기본 주기를 내려준다.
- 필드 자체는 유지한다. 클라이언트가 등록 요청에 이 값을 넘기지 않아도 서버 결과는 같다.
- OCR 프롬프트와 `ReceiptOcrClientResponse`의 추천 주기 필드는 바꾸지 않는다. 서버가 쓰지 않을 뿐이다.

## 파일과 설정

| 파일 | 변경 |
| --- | --- |
| `item/entity/LastReplacementPeriod.kt` | 삭제 |
| `item/dto/ItemDto.kt` | `lastReplacementPeriod`, `newCategoryDefaultReplacementIntervalDays`, `UpdateItemRequest.lastReplacedDate` 삭제, Swagger 설명 갱신 |
| `item/entity/Item.kt` | `update`에서 `lastReplacedDate` 제거 |
| `item/service/ItemService.kt` | `saveItem`, `updateItem` |
| `item/controller/docs/ItemControllerApi.kt` | 구간 선택 설명 제거 |
| `category/entity/Category.kt` | 90일 상수 |
| `category/dto/request/CreateCategoryRequest.kt` | `iconId` 선택값 |
| `category/service/CategoryService.kt` | 생성 경로 두 곳 |
| `receipt/service/ReceiptService.kt`, `receipt/dto/ReceiptDto.kt` | 추천 주기 값과 설명 |

스키마 변경은 없다.

## 호환성

- 구버전 앱이 `lastReplacementPeriod`, `newCategoryDefaultReplacementIntervalDays`, `lastReplacedDate`를 보내도 요청은 성공하고 값은 무시된다. Jackson의 알 수 없는 필드 무시 설정에 기대므로 구현 시 테스트로 확인한다.
- 이미 1일 주기로 만들어진 사용자 소모품과 내 소모품은 보정하지 않는다 (설계 6장).

## 완료 기준

- 단건·일괄·영수증 경로 모두 마지막 교체일이 오늘, 다음 교체일이 오늘 + 주기로 저장된다.
- 구버전 필드를 담은 요청이 실패하지 않고 마지막 교체일은 오늘이다.
- `PATCH /items/{id}`로 마지막 교체일이 바뀌지 않는다. 이름·여분·주기 수정은 그대로 동작한다.
- `POST /categories`와 `newCategoryName` 경로로 만든 사용자 소모품의 주기가 90일, 아이콘이 공용 아이콘이다.
- 영수증 분석 응답의 매칭 실패 소모품 추천 주기가 90이다.
- 영향 테스트 갱신: `ItemCreateAcceptanceTest`, `ItemControllerBulkTest`, `ItemServiceTest` 및 카테고리·영수증 관련 테스트. `./gradlew harness` 통과.
