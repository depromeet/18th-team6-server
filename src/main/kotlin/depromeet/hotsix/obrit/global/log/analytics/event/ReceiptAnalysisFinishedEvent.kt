package depromeet.hotsix.obrit.global.log.analytics.event

/**
 * 영수증 분석이 끝났을 때 남기는 이벤트. 성공·실패 모두 같은 이름으로 적재한다.
 *
 * 성공/실패를 별도 이벤트로 나누면 소요 시간을 볼 때마다 둘을 합쳐야 하므로 하나로 둔다.
 * 사용자 입력값(파일명, 원본 에러 메시지)은 properties에 넣지 않는다. PII 유입 경로가 된다.
 */
data class ReceiptAnalysisFinishedEvent(
    override val userId: Long,
    val success: Boolean,
    val totalMs: Long,
    val ocrMs: Long?,
    val detectedItemCount: Int?,
    val failureReason: ReceiptAnalysisFailureReason?,
) : AnalyticsEvent {
    override val eventName = AnalyticsEventName.RECEIPT_ANALYSIS_FINISHED

    override fun properties(): Map<String, Any?> = mapOf(
        "success" to success,
        "total_ms" to totalMs,
        "ocr_ms" to ocrMs,
        "detected_item_count" to detectedItemCount,
        "failure_reason" to failureReason?.name,
    )
}

/**
 * 영수증 분석 실패 사유. OCR 호출 전 단계의 실패까지 포함한다.
 */
enum class ReceiptAnalysisFailureReason {
    /** 확장자·크기·빈 파일 등 요청 파일 검증 실패. OCR 호출 전에 끝난다 */
    INVALID_FILE,

    /** OCR 서버 5xx 응답 */
    UPSTREAM_5XX,

    /** OCR 호출 타임아웃 */
    TIMEOUT,

    /** OCR 응답에 분석 결과가 없음 */
    EMPTY_RESPONSE,

    /** 위로 분류되지 않는 실패 */
    UNKNOWN,
}
