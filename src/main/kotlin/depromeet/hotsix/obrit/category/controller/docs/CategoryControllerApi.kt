package depromeet.hotsix.obrit.category.controller.docs

import depromeet.hotsix.obrit.category.dto.request.CreateCategoryRequest
import depromeet.hotsix.obrit.category.dto.response.CategoryIconResponse
import depromeet.hotsix.obrit.category.dto.response.CategoryResponse
import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.global.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "카테고리", description = "카테고리 API")
interface CategoryControllerApi {

    @Operation(
        summary = "카테고리 아이콘 전체 조회",
        description = "소모품 종류 등록 시 사용할 수 있는 전체 아이콘 목록을 조회합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "조회 완료",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ApiResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun listCategoryIcons(): ApiResponse<List<CategoryIconResponse>>

    @Operation(
        summary = "카테고리 전체 조회",
        description = "사용자가 접근 가능한 모든 카테고리(기본 제공 + 사용자 등록)를 조회합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "조회 완료",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ApiResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun listCategories(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
    ): ApiResponse<List<CategoryResponse>>

    @Operation(
        summary = "카테고리 생성",
        description = "사용자의 커스텀 카테고리를 생성합니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "201",
                description = "카테고리가 생성되었습니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CategoryResponse::class),
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "유효하지 않은 요청입니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun createCategory(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
        request: CreateCategoryRequest,
    ): ApiResponse<CategoryResponse>

    @Operation(
        summary = "카테고리 삭제",
        description = "커스텀 카테고리와 포함된 항목을 삭제합니다. 기본 제공 카테고리는 삭제할 수 없습니다.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "204", description = "카테고리가 삭제되었습니다."),
            SwaggerApiResponse(
                responseCode = "400",
                description = "기본 제공 카테고리는 삭제할 수 없습니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "카테고리를 찾을 수 없습니다.",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponse::class),
                    ),
                ],
            ),
        ],
    )
    fun deleteCategory(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        userId: Long,
        categoryId: Long,
    )
}
