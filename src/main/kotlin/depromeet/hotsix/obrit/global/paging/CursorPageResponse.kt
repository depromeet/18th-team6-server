package depromeet.hotsix.obrit.global.paging

/**
 * 커서 기반 무한 스크롤 응답에 전체 개수를 포함한 변형.
 *
 * Spring Data의 `Page<T>`처럼 `totalCount`를 제공해 클라이언트가
 * 전체 페이지를 prefetch하지 않고도 상단에 결과 개수를 표시할 수 있다.
 * 전체 개수가 필요 없는 페이지네이션은 [CursorSliceResponse]를 사용한다.
 *
 * 로직: 호출 측이 `size + 1`개를 fetch해서 [fromFetched]에 넘기면,
 * 마지막 1개는 잘라내고 그 존재 여부로 `hasNext`를 판단한다.
 * `totalCount`는 동일 필터 기준 COUNT 쿼리 결과를 별도로 전달받는다.
 */
data class CursorPageResponse<T>(
    val content: List<T>,
    val nextCursor: Long?,
    val size: Int,
    val hasNext: Boolean,
    val totalCount: Long,
) {
    companion object {

        fun <T> fromFetched(
            fetchedContent: List<T>,
            size: Int,
            totalCount: Long,
            cursorSelector: (T) -> Long,
        ): CursorPageResponse<T> {
            val slice = CursorSliceResponse.fromFetched(fetchedContent, size, cursorSelector)

            return CursorPageResponse(
                content = slice.content,
                nextCursor = slice.nextCursor,
                size = slice.size,
                hasNext = slice.hasNext,
                totalCount = totalCount,
            )
        }
    }
}
