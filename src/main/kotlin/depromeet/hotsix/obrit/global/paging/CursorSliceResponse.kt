package depromeet.hotsix.obrit.global.paging

private const val DEFAULT_MAX_PAGE_SIZE = 20

/**
 * 커서 기반 무한 스크롤 응답.
 *
 * Spring Data의 `Slice<T>`와 같은 역할(전체 개수 없이 `hasNext`만 제공)이지만,
 * offset(page number) 대신 마지막 row의 식별자(`nextCursor`)로 다음 페이지를 가져온다.
 *
 * 로직: 호출 측이 `size + 1`개를 fetch해서 [fromFetched]에 넘기면,
 * 마지막 1개는 잘라내고 그 존재 여부로 `hasNext`를 판단한다.
 */
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
