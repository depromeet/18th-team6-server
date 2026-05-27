package depromeet.hotsix.obrit.category.service

import depromeet.hotsix.obrit.category.dto.request.CreateCategoryRequest
import depromeet.hotsix.obrit.category.dto.response.CategoryIconResponse
import depromeet.hotsix.obrit.category.dto.response.CategoryResponse
import depromeet.hotsix.obrit.category.entity.Category
import depromeet.hotsix.obrit.category.repository.CategoryIconRepository
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.global.common.CategoryItemCleaner
import depromeet.hotsix.obrit.global.common.IconUrlResolver
import depromeet.hotsix.obrit.global.exception.BusinessException
import depromeet.hotsix.obrit.global.exception.ConflictException
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val categoryIconRepository: CategoryIconRepository,
    private val userService: UserService,
    private val categoryItemCleaner: CategoryItemCleaner,
    private val iconUrlResolver: IconUrlResolver,
) {

    @Transactional(readOnly = true)
    fun listCategoryIcons(): List<CategoryIconResponse> = categoryIconRepository.findAllByOrderByIdDesc().map {
        CategoryIconResponse(iconId = it.id, url = iconUrlResolver.resolve(it.key))
    }

    @Transactional
    fun createCategory(userId: Long, request: CreateCategoryRequest): CategoryResponse {
        userService.validateUserExist(userId)

        val trimmedName = request.name.trim()
        if (categoryRepository.existsByUserIdIsNullAndNameAndDeletedAtIsNull(trimmedName) ||
            categoryRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, trimmedName)
        ) {
            throw ConflictException("이미 등록된 소모품 종류입니다.")
        }

        val icon = categoryIconRepository.findById(request.iconId)
            .orElseThrow { BusinessException("유효하지 않은 아이콘입니다.") }
        if (icon.deletedAt != null) {
            throw BusinessException("유효하지 않은 아이콘입니다.")
        }

        val category = Category(
            userId = userId,
            name = trimmedName,
            iconId = icon.id,
        )

        return categoryRepository.save(category).toResponse(iconUrlResolver.resolve(icon.key))
    }

    @Transactional
    fun deleteCategory(userId: Long, categoryId: Long) {
        val category = categoryRepository.findActiveById(categoryId)
            ?: throw ResourceNotFoundException("존재하지 않는 소모품 카테고리입니다.")

        if (category.isPreset) {
            throw BusinessException("제공되는 소모품 카테고리는 삭제할 수 없습니다.")
        }

        if (!category.isUserCategoryOf(userId)) {
            throw ResourceNotFoundException("존재하지 않는 소모품 카테고리입니다.")
        }

        category.softDelete()
        categoryItemCleaner.softDeleteActiveItemsByCategory(categoryId, userId)
    }

    private fun Category.toResponse(iconUrl: String): CategoryResponse = CategoryResponse(
        categoryId = requireNotNull(id),
        name = name,
        iconUrl = iconUrl,
        userId = userId,
        createdAt = createdAt,
        defaultReplacementIntervalDays = defaultReplacementIntervalDays,
    )
}
