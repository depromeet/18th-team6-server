package depromeet.hotsix.obrit.notification.controller.docs

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.global.exception.ErrorResponse
import depromeet.hotsix.obrit.notification.dto.response.ListNotificationResponse
import depromeet.hotsix.obrit.notification.dto.response.MarkReadNotificationResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "알림", description = "알림 API")
interface NotificationControllerApi {

    @Operation(
        summary = "알림 목록 조회",
        description = """
사용자의 전체 알림을 최신순으로 조회합니다. 하루 발송 1건이 알림 1개이고, 그 안에 포함된 소모품마다 카드 1개가 `cards`에 담깁니다.

**표시 규칙**
- `isBundled = true`: 카드가 2개 이상인 묶음 알림입니다. 상위 `title`·`content`를 묶음 카드로 보여주고, 펼치면 `cards`를 순서대로 보여줍니다.
- `isBundled = false`: 단건 알림 또는 공지입니다. 카드는 1개입니다.
- `cards`가 빈 배열: 카드 저장 이전에 생성된 기존 알림입니다. 상위 필드만으로 표시합니다.

**카드 정보는 발송 당시 스냅샷입니다**
- `itemName`, `label`, `hasLowStock`, `nextReplacementDate`는 발송 시점 값이며, 이후 소모품을 수정·교체·삭제해도 바뀌지 않습니다.
- `cards`는 급한 순(다음 교체 예상일이 빠른 순)으로 정렬되어 있습니다.

**읽음 상태**
- 읽음은 알림 단위(`isRead`) 하나입니다. 카드별 읽음 상태는 없습니다.
- 묶음을 펼치거나 카드를 누를 때 상위 알림 `id`로 읽음 처리 API를 호출합니다.

**딥링크**
- 카드 `deepLink`: 소모품 카드는 `obrit://items/{itemId}`, 공지 카드는 `obrit://home`입니다.
- 상위 `deepLink`: 단건은 소모품 상세, 묶음·공지·기존 알림은 `obrit://home`입니다.

**삭제된 소모품**
- 발송 후 소모품이 삭제되어도 카드는 목록에 그대로 남습니다.
- 해당 카드의 딥링크로 소모품 상세 조회(`GET /items/{itemId}`)를 호출하면 **404**(`Item not found.`)가 반환되므로, 앱에서 "삭제된 소모품" 안내로 처리해야 합니다.
""",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "조회 완료",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = "*/*",
                        examples = [
                            ExampleObject(
                                name = "묶음 알림과 기존 알림",
                                value = LIST_EXAMPLE,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun listNotification(
        @Parameter(description = "사용자 ID", required = true, example = "1", `in` = ParameterIn.HEADER)
        userId: Long,
    ): ApiResponse<List<ListNotificationResponse>>

    @Operation(
        summary = "알림 읽음 처리",
        description = "특정 알림을 읽음 상태로 변경하고 이동할 딥링크를 반환합니다. 반복 요청에도 읽음 시각은 유지됩니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "읽음 처리 완료"),
            SwaggerApiResponse(
                responseCode = "404",
                description = "존재하지 않는 알림입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun markAsRead(
        @Parameter(description = "사용자 ID", required = true, example = "1", `in` = ParameterIn.HEADER)
        userId: Long,
        @Parameter(description = "알림 ID", required = true, example = "1")
        notificationId: Long,
    ): ApiResponse<MarkReadNotificationResponse>

    @Operation(
        summary = "알림 전체 읽음 처리",
        description = "사용자의 모든 알림을 읽음 상태로 변경합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "전체 읽음 처리 완료"),
        ],
    )
    fun markAsReadAll(
        @Parameter(description = "사용자 ID", required = true, example = "1", `in` = ParameterIn.HEADER)
        userId: Long,
    ): ApiResponse<Nothing?>

    companion object {
        private const val LIST_EXAMPLE = """
{
  "success": true,
  "data": [
    {
      "id": 12,
      "title": "교체가 필요한 소모품이 있어요",
      "content": "수건 외 1건, 확인해보세요",
      "isRead": false,
      "createdAt": "2026-10-27T09:00:00",
      "label": null,
      "nextReplacementDate": null,
      "itemId": null,
      "deepLink": "obrit://home",
      "isBundled": true,
      "cards": [
        {
          "entryId": 31,
          "type": "OVERDUE",
          "title": "교체 시기가 지났어요",
          "content": "수건 교체 예정일이 지났어요 여분도 없어요",
          "itemName": "수건",
          "label": "교체 D+1 · 여분 부족",
          "hasLowStock": true,
          "nextReplacementDate": "2026-10-26",
          "itemId": 10,
          "deepLink": "obrit://items/10"
        },
        {
          "entryId": 32,
          "type": "PRE_REPLACEMENT",
          "title": "교체 시기가 다가와요",
          "content": "칫솔 교체가 얼마 남지 않았어요",
          "itemName": "칫솔",
          "label": "교체 D-3",
          "hasLowStock": false,
          "nextReplacementDate": "2026-10-30",
          "itemId": 20,
          "deepLink": "obrit://items/20"
        }
      ]
    },
    {
      "id": 3,
      "title": "교체 시기가 다가와요",
      "content": "면도기 교체가 얼마 남지 않았어요",
      "isRead": true,
      "createdAt": "2026-09-01T09:00:00",
      "label": null,
      "nextReplacementDate": null,
      "itemId": null,
      "deepLink": "obrit://home",
      "isBundled": false,
      "cards": []
    }
  ]
}
"""
    }
}
