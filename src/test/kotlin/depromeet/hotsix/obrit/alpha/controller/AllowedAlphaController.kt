package depromeet.hotsix.obrit.alpha.controller

import depromeet.hotsix.obrit.alpha.dto.AlphaResponse
import depromeet.hotsix.obrit.alpha.service.AllowedAlphaService

class AllowedAlphaController(private val service: AllowedAlphaService) {
    fun show(response: AlphaResponse): AlphaResponse {
        service.toString()
        return response
    }
}
