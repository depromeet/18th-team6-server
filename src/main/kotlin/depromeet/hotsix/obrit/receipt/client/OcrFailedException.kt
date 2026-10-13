package depromeet.hotsix.obrit.receipt.client

import depromeet.hotsix.obrit.global.exception.BusinessException

/**
 * 영수증 OCR 분석 실패 사유.
 *
 * 실패 원인을 예외 메시지 문자열이 아니라 타입으로 남기기 위해 둔다.
 * 메시지 문구가 바뀌어도 분석 지표는 그대로 유지된다.
 */
enum class OcrFailureReason {
    /** 5xx 등 OCR 서버 응답 오류 */
    UPSTREAM_5XX,

    /** 연결·읽기 타임아웃 */
    TIMEOUT,

    /** 응답은 왔으나 분석 결과가 비어 있음 */
    EMPTY_RESPONSE,

    /** 위로 분류되지 않는 실패 */
    UNKNOWN,
}

/**
 * OCR 분석 실패. [BusinessException]을 상속하므로 API 응답 형태는 기존과 동일하다.
 */
class OcrFailedException(val reason: OcrFailureReason, message: String) : BusinessException(message)
