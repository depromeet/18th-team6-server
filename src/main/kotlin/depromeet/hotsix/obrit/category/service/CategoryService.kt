package depromeet.hotsix.obrit.category.service

import depromeet.hotsix.obrit.category.dto.CategoryResponse
import depromeet.hotsix.obrit.category.dto.CreateCategoryRequest
import depromeet.hotsix.obrit.category.entity.Category
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.global.common.CategoryItemCleaner
import depromeet.hotsix.obrit.global.common.DEFAULT_CATEGORY_IMAGE_URL
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val userService: UserService,
    private val categoryItemCleaner: CategoryItemCleaner,
) {

    @Transactional(readOnly = true)
    fun listCategories(userId: Long): List<CategoryResponse> =
        categoryRepository.findVisibleCategories(userId).map { it.toResponse() }

    @Transactional
    fun createCategory(userId: Long, request: CreateCategoryRequest): CategoryResponse {
        userService.requireExistingUser(userId)
        val imageUrl = request.imageUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_CATEGORY_IMAGE_URL

        val category = Category(
            userId = userId,
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
        if (category.userId != userId) {
            throw ResourceNotFoundException("Category not found.")
        }

        category.softDelete()
        categoryItemCleaner.softDeleteActiveItemsByCategory(categoryId, userId)
    }

    private fun Category.toResponse(): CategoryResponse = CategoryResponse(
        id = requireNotNull(id),
        name = name,
        imageUrl = imageUrl,
        defaultReplacementIntervalDays = defaultReplacementIntervalDays,
        preset = isPreset,
    )
}
