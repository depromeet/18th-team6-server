package depromeet.hotsix.obrit.item.dto

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Constraint(validatedBy = [EitherCategoryIdOrNewCategoryNameValidator::class])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EitherCategoryIdOrNewCategoryName(
    val message: String = "categoryId와 newCategoryName 중 정확히 하나만 제공해야 합니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class EitherCategoryIdOrNewCategoryNameValidator :
    ConstraintValidator<EitherCategoryIdOrNewCategoryName, CreateItemRequest> {
    override fun isValid(value: CreateItemRequest?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return true
        val hasCategoryId = value.categoryId != null
        val hasNewCategoryName = value.newCategoryName != null
        return hasCategoryId != hasNewCategoryName
    }
}
