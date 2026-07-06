package depromeet.hotsix.obrit.global.exception

import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(exception: BusinessException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(exception.message.orEmpty()))

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(exception: ResourceNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(exception.message.orEmpty()))

    @ExceptionHandler(ConflictException::class)
    fun handleConflictException(exception: ConflictException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(exception.message.orEmpty()))

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(exception: ForbiddenException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse(exception.message.orEmpty()))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(exception: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Invalid request."
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(message))
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidationException(
        exception: HandlerMethodValidationException,
    ): ResponseEntity<ErrorResponse> {
        val message = exception.allErrors.firstOrNull()?.defaultMessage ?: "Invalid request."
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(message))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(exception: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(exception.message ?: "잘못된 요청입니다."))

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameterException(
        exception: MissingServletRequestParameterException,
    ): ResponseEntity<ErrorResponse> {
        val message = "필수 요청 파라미터가 누락되었습니다: ${exception.parameterName}"
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(message))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleArgumentTypeMismatch(exception: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        val message = "요청 파라미터 타입이 올바르지 않습니다: ${exception.name}"
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(message))
    }

    @ExceptionHandler(LogTailTimeoutException::class)
    fun handleLogTailTimeoutException(exception: LogTailTimeoutException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(ErrorResponse(exception.message.orEmpty()))

    @ExceptionHandler(Exception::class)
    fun handleGenericException(exception: Exception): ResponseEntity<ErrorResponse> {
        logger.error("예상치 못한 오류가 발생했습니다.", exception)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse("예상치 못한 오류가 발생했습니다."))
    }
}

@Schema(description = "에러 응답")
data class ErrorResponse(
    @field:Schema(description = "에러 메시지", example = "존재하지 않는 소모품 카테고리입니다.")
    val message: String,
)

class BusinessException(message: String) : RuntimeException(message)

class ResourceNotFoundException(message: String) : RuntimeException(message)

class ConflictException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class LogTailTimeoutException(message: String) : RuntimeException(message)

class ForbiddenException(message: String) : RuntimeException(message)
