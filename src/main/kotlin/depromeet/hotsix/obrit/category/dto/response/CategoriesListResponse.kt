package depromeet.hotsix.obrit.category.dto.response

data class CategoriesListResponse(val totalCount: Int, val items: List<CategoryResponse>, val nextCursor: Long?)
