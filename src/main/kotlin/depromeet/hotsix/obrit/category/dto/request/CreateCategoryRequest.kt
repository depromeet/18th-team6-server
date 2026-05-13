package depromeet.hotsix.obrit.category.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreateCategoryRequest(
    @field:NotBlank(message = "종류명은 필수입니다.")
    @field:Size(max = 15, message = "종류명은 최대 15자입니다.")
    @field:Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "종류명은 한글/영문만 입력 가능합니다.")
    val name: String,

    @field:NotNull(message = "아이콘을 선택해주세요.")
    @field:Positive(message = "유효하지 않은 아이콘입니다.")
    val iconId: Long,
)
