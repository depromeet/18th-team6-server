package depromeet.hotsix.obrit.notification.controller

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.notification.controller.docs.NotificationSettingsControllerApi
import depromeet.hotsix.obrit.notification.dto.request.UpdateNotificationSettingsRequest
import depromeet.hotsix.obrit.notification.dto.response.NotificationSettingsResponse
import depromeet.hotsix.obrit.notification.service.UserNotificationSettingsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/notifications/settings")
class NotificationSettingsController(private val userNotificationSettingsService: UserNotificationSettingsService) :
    NotificationSettingsControllerApi {

    @GetMapping
    override fun getSettings(@RequestHeader("X-User-Id") userId: Long): ApiResponse<NotificationSettingsResponse> {
        val result = userNotificationSettingsService.getSettings(userId)
        return ApiResponse.ok(result)
    }

    @PutMapping
    override fun updateSettings(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestBody request: UpdateNotificationSettingsRequest,
    ): ApiResponse<NotificationSettingsResponse> {
        val result = userNotificationSettingsService.updateSettings(userId, request)
        return ApiResponse.ok(result)
    }
}
