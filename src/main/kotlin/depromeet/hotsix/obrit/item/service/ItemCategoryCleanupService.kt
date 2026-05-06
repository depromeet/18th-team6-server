package depromeet.hotsix.obrit.item.service

import depromeet.hotsix.obrit.global.common.CategoryItemCleaner
import depromeet.hotsix.obrit.item.repository.ItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ItemCategoryCleanupService(private val itemRepository: ItemRepository) : CategoryItemCleaner {

    @Transactional
    override fun softDeleteActiveItemsByCategory(categoryId: Long, userId: Long) {
        itemRepository.findActiveByCategoryIdAndUserId(categoryId, userId).forEach { it.softDelete() }
    }
}
