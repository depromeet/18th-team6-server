package depromeet.hotsix.obrit.user.controller

import depromeet.hotsix.obrit.global.dto.ApiResponse
import depromeet.hotsix.obrit.user.controller.docs.UserControllerApi
import depromeet.hotsix.obrit.user.dto.request.RegisterUserRequest
import depromeet.hotsix.obrit.user.dto.response.RegisterUserResponse
import depromeet.hotsix.obrit.user.service.UserService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(private val userService: UserService) : UserControllerApi {

    @PostMapping
    override fun register(@RequestBody request: RegisterUserRequest): ApiResponse<RegisterUserResponse> =
        ApiResponse.ok(userService.registerOrGet(request))
}
