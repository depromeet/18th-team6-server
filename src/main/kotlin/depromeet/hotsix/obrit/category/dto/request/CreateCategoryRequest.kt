package depromeet.hotsix.obrit.category.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

@Schema(description = "카테고리 생성 요청")
data class CreateCategoryRequest(
    @field:Schema(description = "종류명 (한글/영문, 최대 15자)", example = "수건")
    @field:NotBlank(message = "종류명은 필수입니다.")
    @field:Size(max = 15, message = "종류명은 최대 15자입니다.")
    @field:Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "종류명은 한글/영문만 입력 가능합니다.")
    val name: String,

    @field:Schema(description = "아이콘 ID", example = "3")
    @field:NotNull(message = "아이콘을 선택해주세요.")
    @field:Positive(message = "유효하지 않은 아이콘입니다.")
    val iconId: Long,
)
