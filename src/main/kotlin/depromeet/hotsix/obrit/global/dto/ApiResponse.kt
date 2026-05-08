package depromeet.hotsix.obrit.global.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(val success: Boolean, val message: String? = null, val data: T? = null) {
    companion object {
        fun <T> ok(data: T) = ApiResponse(true, null, data)

        fun <T> ok(message: String, data: T) = ApiResponse(true, message, data)

        fun <T> fail(message: String) = ApiResponse<T>(false, message, null)
    }
}
