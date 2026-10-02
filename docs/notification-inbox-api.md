# 알림함 API 변경

기존 목록·읽음 API 경로 및 응답 필드는 유지한다. 목록 응답에는 묶음 상세 카드 필드를 추가한다.

| API | 추가 필드 | 의미 |
| --- | --- | --- |
| 목록 | `label` | 발송 당시 라벨: `여분 부족`, `교체 D-3`, `교체 D+3` 등 |
| 목록 | `nextReplacementDate` | 발송 당시 다음 교체 예상일 (`yyyy-MM-dd`) |
| 목록·읽음 | `itemId` | 단건 알림의 대상 소모품 ID |
| 목록·읽음 | `deepLink` | 클릭 시 이동할 앱 URI |
| 목록 | `isBundled` | 상세 카드가 둘 이상인지 여부 |
| 목록 | `cards` | 발송에 포함된 소모품별 상세 카드 |

라벨과 교체 예상일은 발송 당시 스냅샷이다. 이후 소모품을 교체하거나 수정해도 과거 알림 내용은 변하지 않는다.
묶음·공지·기존 알림의 상위 `label`, `nextReplacementDate`, `itemId`는 기존 규칙을 유지한다.
신규 알림은 발송에 포함된 후보마다 `cards`에 타입, 라벨, 소모품 ID와 딝링크를 내려준다.
기존 알림은 엔트리를 백필하지 않으므로 `cards`가 빈 목록이다.

## 앱 연동

딥링크 기본값은 **앱 측 확인 전 임시 계약**이다.

- 카드의 소모품 ID가 있음: `obrit://items/{itemId}`
- 공지 카드 또는 기존 상위 묶음 알림: `obrit://home`
- 설정: `notification.deep-link.item-template`, `notification.deep-link.home`

앱에서 이 URI를 처리하는 라우터를 연결해야 실제 화면 이동이 동작한다.
대상이 삭제된 경우 상세 API의 404를 앱에서 처리해야 한다.
푸시의 FCM `data`에도 문자열 `deepLink`를 제공한다.
알림함의 개별 카드는 해당 카드의 `deepLink`로 이동한다.
푸시 클릭 시 `deepLink`로 이동한다. 배치 알림은 전송 성공 후 이력을 생성하므로 푸시에 알림 ID는 포함하지 않는다.

## DB

`V12__add_notification_entries.sql`이 부모 알림의 개별 카드를 저장할 `notification_entries`를 추가한다.
기존 `notifications` 컬럼은 호환을 위해 유지하고 신규 알림부터 부모와 엔트리를 함께 저장한다.
배포 시 애플리케이션 기동 전에 Flyway 적용이 필요하다.
