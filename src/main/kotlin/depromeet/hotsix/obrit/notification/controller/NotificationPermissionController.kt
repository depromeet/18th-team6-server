package depromeet.hotsix.obrit.notification.controller

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.notification.controller.docs.NotificationPermissionControllerApi
import depromeet.hotsix.obrit.notification.dto.request.ReportNotificationPermissionRequest
import depromeet.hotsix.obrit.notification.dto.response.NotificationSettingsResponse
import depromeet.hotsix.obrit.notification.service.UserNotificationSettingsService
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/notifications/permission")
class NotificationPermissionController(private val userNotificationSettingsService: UserNotificationSettingsService) :
    NotificationPermissionControllerApi {

    @PutMapping
    override fun reportPermission(
        @RequestHeader("X-User-Id") userId: Long,
        @RequestBody request: ReportNotificationPermissionRequest,
    ): ApiResponse<NotificationSettingsResponse> {
        val result = userNotificationSettingsService.reportPermission(userId, request)
        return ApiResponse.ok(result)
    }
}
