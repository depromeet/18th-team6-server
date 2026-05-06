package depromeet.hotsix.obrit.category.service

import depromeet.hotsix.obrit.category.dto.request.CreateCategoryRequest
import depromeet.hotsix.obrit.category.dto.response.CategoriesListResponse
import depromeet.hotsix.obrit.category.dto.response.CategoryResponse
import depromeet.hotsix.obrit.category.entity.Category
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.global.common.CategoryItemCleaner
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.user.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val userService: UserService,
    private val categoryItemCleaner: CategoryItemCleaner,
) {

    @Transactional(readOnly = true)
    fun listUserCategoriesWithPagination(userId: Long, cursor: Long = 0L, limit: Int = 20): CategoriesListResponse {
        userService.validateUserExist(userId)
        val pageable = PageRequest.of(0, limit, Sort.by("id").ascending())
        val result = categoryRepository.findByUserIdWithCursor(userId, cursor, pageable)
        val items = result.content.map { it.toResponse() }
        val nextCursor = if (result.hasNext()) items.lastOrNull()?.id else null

        return CategoriesListResponse(
            totalCount = items.size,
            items = items,
            nextCursor = nextCursor,
        )
    }

    @Transactional(readOnly = true)
    fun listPresetCategoriesWithPagination(cursor: Long = 0L, limit: Int = 20): CategoriesListResponse {
        val pageable = PageRequest.of(0, limit, Sort.by("id").ascending())
        val result = categoryRepository.findByUserIdIsNullWithCursor(cursor, pageable)
        val items = result.content.map { it.toResponse() }
        val nextCursor = if (result.hasNext()) items.lastOrNull()?.id else null

        return CategoriesListResponse(
            totalCount = items.size,
            items = items,
            nextCursor = nextCursor,
        )
    }

    @Transactional(readOnly = true)
    fun listAllAccessibleCategories(userId: Long): List<CategoryResponse> {
        userService.validateUserExist(userId)
        val pageable = PageRequest.of(0, 1000, Sort.by("id").ascending())
        val userCategories = categoryRepository.findByUserIdWithCursor(userId, 0L, pageable).content
        val presetCategories = categoryRepository.findByUserIdIsNullAndDeletedAtIsNull(pageable).content

        return (presetCategories + userCategories).map { it.toResponse() }
    }

    @Transactional
    fun createCategory(userId: Long, request: CreateCategoryRequest): CategoryResponse {
        userService.validateUserExist(userId)
        val imageUrl = request.imageUrl?.takeIf { it.isNotBlank() } ?: "/images/default-category.png"

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
