package depromeet.hotsix.obrit.notification.controller

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.notification.dto.request.RegisterFcmTokenRequest
import depromeet.hotsix.obrit.notification.dto.response.RegisterFcmTokenResponse
import depromeet.hotsix.obrit.notification.service.FcmTokenService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/fcm-tokens")
class FcmTokenController(private val fcmTokenService: FcmTokenService) {
    @PostMapping
    fun registerToken(
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: RegisterFcmTokenRequest,
    ): ApiResponse<RegisterFcmTokenResponse> {
        val response = fcmTokenService.registerToken(userId, request.token)
        return ApiResponse.ok(response)
    }
}
