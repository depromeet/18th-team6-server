package depromeet.hotsix.obrit.category.dto.response

import depromeet.hotsix.obrit.category.entity.CategoryGroupType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "소모품 카테고리(욕실·주방 등) 응답")
data class CategoryGroupResponse(
    @field:Schema(description = "카테고리 ID", example = "1")
    val categoryGroupId: Long,

    @field:Schema(description = "카테고리 이름", example = "욕실")
    val name: String,

    @field:Schema(description = "카테고리 종류 (PRESET: 기본 소모품 묶음, USER: 사용자가 만든 소모품 묶음)", example = "PRESET")
    val type: CategoryGroupType,

    @field:Schema(description = "탭 노출 순서 (오름차순)", example = "1")
    val displayOrder: Int,
)
