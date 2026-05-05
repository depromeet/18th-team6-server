package depromeet.hotsix.obrit.common

interface CategoryItemCleaner {
    fun softDeleteActiveItemsByCategory(categoryId: Long, userId: Long)
}
