package depromeet.hotsix.obrit.global.exception

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(exception: BusinessException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(exception.message.orEmpty()))

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(exception: ResourceNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(exception.message.orEmpty()))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(exception: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Invalid request."
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(message))
    }
}

@Schema(description = "Error response.")
data class ErrorResponse(
    @field:Schema(description = "Human-readable error message.", example = "Category not found.")
    val message: String,
)

class BusinessException(message: String) : RuntimeException(message)

class ResourceNotFoundException(message: String) : RuntimeException(message)
