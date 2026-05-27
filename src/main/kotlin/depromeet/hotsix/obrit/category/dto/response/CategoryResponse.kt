package depromeet.hotsix.obrit.category.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "카테고리 응답")
data class CategoryResponse(
    @field:Schema(description = "카테고리 ID", example = "100")
    val categoryId: Long,

    @field:Schema(description = "카테고리 이름", example = "칫솔")
    val name: String,

    @field:Schema(description = "카테고리 아이콘 URL", example = "https://cdn.obrit.com/icons/toothbrush.png")
    val iconUrl: String,

    @field:Schema(description = "사용자 ID (기본 제공 카테고리는 null)", example = "1", nullable = true)
    val userId: Long?,

    @field:Schema(description = "생성일시", example = "2026-05-01T10:30:00", nullable = true)
    val createdAt: LocalDateTime?,

    @field:Schema(description = "기본 교체 주기 (일)", example = "90")
    val defaultReplacementIntervalDays: Int,
)
