package depromeet.hotsix.obrit.category.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "카테고리 아이콘 응답")
data class CategoryIconResponse(
    @field:Schema(description = "아이콘 ID", example = "1")
    val id: Long,

    @field:Schema(description = "아이콘 URL", example = "https://cdn.obrit.com/icons/toothbrush.png")
    val url: String,
)
