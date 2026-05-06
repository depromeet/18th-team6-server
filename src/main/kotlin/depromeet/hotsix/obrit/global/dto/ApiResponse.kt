package depromeet.hotsix.obrit.global.dto

data class ApiResponse<T>(val success: Boolean, val data: T? = null, val message: String? = null)
