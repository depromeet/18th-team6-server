package depromeet.hotsix.obrit.category.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "카테고리 생성 요청")
data class CreateCategoryRequest(
    @field:Schema(description = "종류명 (한글/영문/숫자, 최대 15자)", example = "수건")
    @field:NotBlank(message = "종류명은 필수입니다.")
    @field:Size(max = 15, message = "종류명은 최대 15자입니다.")
    @field:Pattern(regexp = "^[가-힣a-zA-Z0-9\\s]+$", message = "종류명은 한글/영문/숫자만 입력 가능합니다.")
    val name: String,

    @field:Schema(
        description = "사용하지 않음. 구버전 앱 호환을 위해 받기만 하며, 아이콘은 항상 공용 아이콘으로 저장됩니다.",
        example = "1",
        deprecated = true,
    )
    val iconId: Long? = null,
)
