package depromeet.hotsix.obrit.notification.controller

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.notification.service.NotificationService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/notifications")
class NotificationController(private val notificationService: NotificationService) {

    @PutMapping("/{notificationId}/read")
    fun markAsRead(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable notificationId: Long,
    ): ApiResponse<Nothing?> {
        notificationService.markAsRead(userId, notificationId)
        return ApiResponse.ok(null)
    }

    @PutMapping("/read-all")
    fun markAsReadAll(@RequestHeader("X-User-Id") userId: Long): ApiResponse<Nothing?> {
        notificationService.markAsReadAll(userId)
        return ApiResponse.ok(null)
    }
}
