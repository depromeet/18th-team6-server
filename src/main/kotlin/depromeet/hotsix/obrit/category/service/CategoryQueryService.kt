package depromeet.hotsix.obrit.category.service

import depromeet.hotsix.obrit.category.entity.Category
import depromeet.hotsix.obrit.category.repository.CategoryRepository
import depromeet.hotsix.obrit.global.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CategoryQueryService(private val categoryRepository: CategoryRepository) {

    fun getVisibleCategoryNameAndDefaultInterval(userId: Long, categoryId: Long): Pair<String, Int> {
        val category = findVisibleCategory(userId, categoryId)
        return category.name to category.defaultReplacementIntervalDays
    }

    fun getVisibleCategoryName(userId: Long, categoryId: Long): String = findVisibleCategory(userId, categoryId).name

    fun findVisibleCategoryNames(userId: Long, categoryIds: Collection<Long>): Map<Long, String> {
        if (categoryIds.isEmpty()) {
            return emptyMap()
        }

        return categoryRepository.findVisibleByIds(userId, categoryIds.toSet())
            .associate { requireNotNull(it.id) to it.name }
    }

    private fun findVisibleCategory(userId: Long, categoryId: Long): Category {
        val category = categoryRepository.findActiveById(categoryId)
            ?: throw ResourceNotFoundException("Category not found.")

        if (category.userId != null && category.userId != userId) {
            throw ResourceNotFoundException("Category not found.")
        }

        return category
    }
}
