package depromeet.hotsix.obrit.notification.controller

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.notification.controller.docs.DeviceControllerApi
import depromeet.hotsix.obrit.notification.dto.request.RegisterDeviceRequest
import depromeet.hotsix.obrit.notification.dto.response.RegisterDeviceResponse
import depromeet.hotsix.obrit.notification.service.DeviceRegistrationService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/notifications/devices")
class DeviceController(private val deviceRegistrationService: DeviceRegistrationService) : DeviceControllerApi {

    @PostMapping
    override fun registerDevice(
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: RegisterDeviceRequest,
    ): ApiResponse<RegisterDeviceResponse> = ApiResponse.ok(deviceRegistrationService.register(userId, request.fid))

    @DeleteMapping("/{fid}")
    override fun unregisterDevice(
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable fid: String,
    ): ApiResponse<Nothing?> {
        deviceRegistrationService.unregister(userId, fid)
        return ApiResponse.ok(null)
    }
}
