package depromeet.hotsix.obrit.category

import depromeet.hotsix.obrit.common.BusinessException
import depromeet.hotsix.obrit.common.CategoryItemCleaner
import depromeet.hotsix.obrit.common.DEFAULT_CATEGORY_IMAGE_URL
import depromeet.hotsix.obrit.common.ResourceNotFoundException
import depromeet.hotsix.obrit.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository,
    private val categoryItemCleaner: CategoryItemCleaner,
) {

    @Transactional(readOnly = true)
    fun listCategories(userId: Long): List<CategoryResponse> =
        categoryRepository.findVisibleCategories(userId).map { it.toResponse() }

    @Transactional
    fun createCategory(userId: Long, request: CreateCategoryRequest): CategoryResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found.") }
        val imageUrl = request.imageUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_CATEGORY_IMAGE_URL

        val category = Category(
            user = user,
            name = request.name.trim(),
            imageUrl = imageUrl,
            defaultReplacementIntervalDays = request.defaultReplacementIntervalDays,
        )

        return categoryRepository.save(category).toResponse()
    }

    @Transactional
    fun deleteCategory(userId: Long, categoryId: Long) {
        val category = categoryRepository.findActiveById(categoryId)
            ?: throw ResourceNotFoundException("Category not found.")

        if (category.isPreset) {
            throw BusinessException("Preset categories cannot be deleted.")
        }
        if (category.user?.id != userId) {
            throw ResourceNotFoundException("Category not found.")
        }

        category.softDelete()
        categoryItemCleaner.softDeleteActiveItemsByCategory(categoryId, userId)
    }
}
