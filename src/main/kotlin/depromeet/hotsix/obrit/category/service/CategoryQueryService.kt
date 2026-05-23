package depromeet.hotsix.obrit.category.service

import depromeet.hotsix.obrit.category.dto.response.CategoryResponse
import depromeet.hotsix.obrit.category.entity.Category
import depromeet.hotsix.obrit.category.entity.CategorySnapshot
import depromeet.hotsix.obrit.category.repository.CategoryIconRepository
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import depromeet.hotsix.obrit.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CategoryQueryService(
    private val categoryRepository: CategoryRepository,
    private val categoryIconRepository: CategoryIconRepository,
    private val userService: UserService,
) {

    fun listAllAccessibleCategories(userId: Long): List<CategoryResponse> {
        userService.validateUserExist(userId)
        val presetCategories = categoryRepository.findActivePresets()
        val userCategories = categoryRepository.findActiveByUserId(userId)
        val allCategories = presetCategories + userCategories

        val iconIds = allCategories.map { it.iconId }.distinct()
        val iconUrlMap = categoryIconRepository.findAllById(iconIds).associate { it.id to it.url }

        return allCategories.map { it.toResponse(iconUrlMap[it.iconId].orEmpty()) }
    }

    fun getVisibleCategoryNameAndDefaultInterval(userId: Long, categoryId: Long): Pair<String, Int> {
        val category = findVisibleCategory(userId, categoryId)
        return category.name to category.defaultReplacementIntervalDays
    }

    fun getVisibleCategoryName(userId: Long, categoryId: Long): String = findVisibleCategory(userId, categoryId).name

    fun getVisibleCategorySnapshot(userId: Long, categoryId: Long): CategorySnapshot {
        val category = findVisibleCategory(userId, categoryId)
        val iconUrl = categoryIconRepository.findById(category.iconId)
            .map { it.url }
            .orElse("")

        return CategorySnapshot(
            id = requireNotNull(category.id),
            name = category.name,
            iconUrl = iconUrl,
            defaultReplacementIntervalDays = category.defaultReplacementIntervalDays,
        )
    }

    fun findVisibleCategoryNames(userId: Long, categoryIds: Collection<Long>): Map<Long, String> {
        if (categoryIds.isEmpty()) {
            return emptyMap()
        }

        return categoryRepository.findVisibleByIds(userId, categoryIds.toSet())
            .associate { requireNotNull(it.id) to it.name }
    }

    private fun Category.toResponse(iconUrl: String): CategoryResponse = CategoryResponse(
        id = requireNotNull(id),
        name = name,
        iconUrl = iconUrl,
        userId = userId,
        createdAt = createdAt,
        defaultReplacementIntervalDays = defaultReplacementIntervalDays,
    )

    private fun findVisibleCategory(userId: Long, categoryId: Long): Category {
        val category = categoryRepository.findActiveById(categoryId)
            ?: throw ResourceNotFoundException("존재하지 않는 소모품 카테고리입니다.")

        if (category.userId != null && category.userId != userId) {
            throw ResourceNotFoundException("존재하지 않는 소모품 카테고리입니다.")
        }

        return category
    }
}
