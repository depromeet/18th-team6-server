package depromeet.hotsix.obrit.global.common

interface CategoryItemCleaner {
    fun softDeleteActiveItemsByCategory(categoryId: Long, userId: Long)
}
