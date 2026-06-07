package depromeet.hotsix.obrit.item.controller.docs

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.global.exception.ErrorResponse
import depromeet.hotsix.obrit.item.dto.BulkCreateItemRequest
import depromeet.hotsix.obrit.item.dto.CreateItemRequest
import depromeet.hotsix.obrit.item.dto.CreateReplacementRequest
import depromeet.hotsix.obrit.item.dto.ItemDetailResponse
import depromeet.hotsix.obrit.item.dto.ItemResponse
import depromeet.hotsix.obrit.item.dto.ReplacementHistoryResponse
import depromeet.hotsix.obrit.item.dto.UpdateItemRequest
import depromeet.hotsix.obrit.item.dto.UpdateSpareCountRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "소모품", description = "소모품 API")
interface ItemControllerApi {

    @Operation(
        summary = "소모품 목록 조회",
        description = "사용자의 활성 소모품 목록을 다음 교체일 오름차순으로 조회합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "조회 완료",
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "존재하지 않는 사용자입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "사용자 없음",
                                value = """{"message": "존재하지 않는 사용자입니다."}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun listItems(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
    ): ApiResponse<List<ItemResponse>>

    @Operation(
        summary = "소모품 상세 조회",
        description = "소모품의 종류, 대표 이미지, 교체 상태, 사용 현황, 최근 교체 기록을 조회합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "조회 완료",
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "존재하지 않는 소모품입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "소모품 없음",
                                value = """{"message": "Item not found."}""",
                            ),
                            ExampleObject(
                                name = "카테고리 없음",
                                value = """{"message": "존재하지 않는 소모품 카테고리입니다."}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getItemDetail(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
        @Parameter(description = "소모품 ID", required = true, example = "1")
        itemId: Long,
    ): ApiResponse<ItemDetailResponse>

    @Operation(
        summary = "소모품 단건 등록",
        description = """기본 제공 카테고리 또는 사용자 커스텀 카테고리에 소모품을 등록합니다.

### 마지막 교체 시기 (lastReplacementPeriod)
선택지별 기간의 평균치를 교체일자로 적용합니다.

| 선택지 | enum | 적용 날짜 |
|--------|------|-----------|
| 1주일 이내 | WITHIN_WEEK | 4일 전 |
| 2-4주 전 | WITHIN_MONTH | 21일 전 |
| 1-3개월 전 | WITHIN_THREE_MONTHS | 45일 전 |
| 3개월 이전 | OVER_THREE_MONTHS | 90일 전 |
| 미선택 | null | 오늘 |""",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "201",
                description = "소모품이 등록되었습니다.",
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "유효하지 않은 요청입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "소모품명 누락",
                                value = """{"message": "소모품명은 필수입니다."}""",
                            ),
                            ExampleObject(
                                name = "수량 오류",
                                value = """{"message": "수량은 0 이상이어야 합니다."}""",
                            ),
                        ],
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "존재하지 않는 카테고리입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "카테고리 없음",
                                value = """{"message": "존재하지 않는 소모품 카테고리입니다."}""",
                            ),
                        ],
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "409",
                description = "이미 등록된 소모품 이름입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "중복 이름",
                                value = """{"message": "이미 등록된 소모품 이름입니다."}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun createItem(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
        @Valid request: CreateItemRequest,
    ): ApiResponse<ItemResponse>

    @Operation(
        summary = "소모품 다건 등록",
        description = """한 번의 요청으로 여러 소모품을 등록합니다. 최소 1개, 최대 20개까지 등록할 수 있으며, 하나라도 실패하면 전체 롤백됩니다.

### 마지막 교체 시기 (lastReplacementPeriod)
선택지별 기간의 평균치를 교체일자로 적용합니다.

| 선택지 | enum | 적용 날짜 |
|--------|------|-----------|
| 1주일 이내 | WITHIN_WEEK | 4일 전 |
| 2-4주 전 | WITHIN_MONTH | 21일 전 |
| 1-3개월 전 | WITHIN_THREE_MONTHS | 45일 전 |
| 3개월 이전 | OVER_THREE_MONTHS | 90일 전 |
| 미선택 | null | 오늘 |""",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "201",
                description = "소모품들이 등록되었습니다. 응답 배열은 요청 순서와 동일합니다.",
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "유효하지 않은 요청입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "목록 크기 초과",
                                value = """{"message": "소모품 목록은 1개 이상 20개 이하여야 합니다."}""",
                            ),
                            ExampleObject(
                                name = "목록 누락",
                                value = """{"message": "소모품 목록은 필수입니다."}""",
                            ),
                        ],
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "존재하지 않는 카테고리 또는 사용자입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "카테고리 없음",
                                value = """{"message": "존재하지 않는 소모품 카테고리입니다."}""",
                            ),
                        ],
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "409",
                description = "이름 중복으로 등록 불가합니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "요청 내 중복",
                                value = """{"message": "요청에 중복된 소모품 이름이 있습니다."}""",
                            ),
                            ExampleObject(
                                name = "기존 데이터 중복",
                                value = """{"message": "이미 등록된 소모품 이름입니다."}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun bulkCreateItems(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
        @Valid request: BulkCreateItemRequest,
    ): ApiResponse<List<ItemResponse>>

    @Operation(
        summary = "소모품 수정",
        description = "소모품의 이름, 수량, 교체 주기, 마지막 교체일 등 수정 가능한 필드를 변경합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "소모품이 수정되었습니다.",
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "유효하지 않은 요청입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "소모품명 공백",
                                value = """{"message": "Item name cannot be blank."}""",
                            ),
                        ],
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "존재하지 않는 소모품입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "소모품 없음",
                                value = """{"message": "Item not found."}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateItem(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
        @Parameter(description = "소모품 ID", required = true, example = "1")
        itemId: Long,
        @Valid request: UpdateItemRequest,
    ): ApiResponse<ItemResponse>

    @Operation(
        summary = "소모품 여분 수량 수정",
        description = "소모품의 여분 수량만 변경합니다. 수량은 0 이상이어야 하며 교체일, 다음 교체일, 교체 주기는 변경하지 않습니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "여분 수량이 수정되었습니다.",
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "유효하지 않은 요청입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "수량 누락",
                                value = """{"message": "여분 수량은 필수입니다."}""",
                            ),
                            ExampleObject(
                                name = "음수 수량",
                                value = """{"message": "여분 수량은 0 이상이어야 합니다."}""",
                            ),
                        ],
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "존재하지 않는 소모품입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "소모품 없음",
                                value = """{"message": "Item not found."}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateSpareCount(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
        @Parameter(description = "소모품 ID", required = true, example = "1")
        itemId: Long,
        @Valid request: UpdateSpareCountRequest,
    ): ApiResponse<ItemResponse>

    @Operation(
        summary = "소모품 삭제",
        description = "소모품을 소프트 삭제합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "204", description = "소모품이 삭제되었습니다."),
            SwaggerApiResponse(
                responseCode = "404",
                description = "존재하지 않는 소모품입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "소모품 없음",
                                value = """{"message": "Item not found."}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun deleteItem(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
        @Parameter(description = "소모품 ID", required = true, example = "1")
        itemId: Long,
    )

    @Operation(
        summary = "소모품 교체 기록",
        description = "소모품의 교체일을 기록하고 다음 교체일을 자동으로 갱신합니다. 교체일을 생략하면 오늘 날짜로 기록됩니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "201",
                description = "교체 기록이 추가되었습니다.",
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "존재하지 않는 소모품입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "소모품 없음",
                                value = """{"message": "Item not found."}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun replaceItem(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
        @Parameter(description = "소모품 ID", required = true, example = "1")
        itemId: Long,
        request: CreateReplacementRequest,
    ): ApiResponse<ItemResponse>

    @Operation(
        summary = "소모품 교체 이력 조회",
        description = "특정 소모품의 최근 교체 이력을 최신순으로 조회합니다. limit은 1~5 사이로 지정 가능하며 생략 시 5건을 반환합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "조회 완료. 이력이 limit보다 적으면 있는 만큼만 반환합니다.",
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "유효하지 않은 요청입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "limit 범위 초과",
                                value = """{"message": "limit은 1 이상 5 이하여야 합니다."}""",
                            ),
                        ],
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "존재하지 않는 소모품입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(
                                name = "소모품 없음",
                                value = """{"message": "Item not found."}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun listReplacements(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
        @Parameter(description = "소모품 ID", required = true, example = "1")
        itemId: Long,
        @Parameter(description = "조회 개수(1~5). 기본 5.", example = "5")
        @Min(value = 1, message = "limit은 1 이상 5 이하여야 합니다.")
        @Max(value = 5, message = "limit은 1 이상 5 이하여야 합니다.")
        limit: Int,
    ): ApiResponse<List<ReplacementHistoryResponse>>
}
