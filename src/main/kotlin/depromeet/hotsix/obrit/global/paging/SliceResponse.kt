package depromeet.hotsix.obrit.global.paging

private const val DEFAULT_MAX_PAGE_SIZE = 20

data class CursorSliceResponse<T>(val content: List<T>, val nextCursor: Long?, val size: Int, val hasNext: Boolean) {
    companion object {

        fun <T> fromFetched(fetchedContent: List<T>, size: Int, cursorSelector: (T) -> Long): CursorSliceResponse<T> {
            val normalizedSize = normalizePageSize(size)
            val content = fetchedContent.take(normalizedSize)
            val hasNext = fetchedContent.size > normalizedSize

            return CursorSliceResponse(
                content = content,
                nextCursor = content.lastOrNull()?.let(cursorSelector)?.takeIf { hasNext },
                size = normalizedSize,
                hasNext = hasNext,
            )
        }
    }
}

fun normalizePageSize(size: Int, maxSize: Int = DEFAULT_MAX_PAGE_SIZE): Int = size.coerceIn(1, maxSize)
