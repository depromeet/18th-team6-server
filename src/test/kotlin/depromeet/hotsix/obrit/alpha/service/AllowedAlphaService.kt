package depromeet.hotsix.obrit.alpha.service

import depromeet.hotsix.obrit.alpha.dto.AlphaResponse
import depromeet.hotsix.obrit.alpha.entity.AlphaEntity
import depromeet.hotsix.obrit.alpha.repository.AlphaRepository
import depromeet.hotsix.obrit.beta.service.BetaService

class AllowedAlphaService(private val repository: AlphaRepository, private val betaService: BetaService) {
    fun execute(): AlphaResponse {
        repository.toString()
        betaService.toString()
        AlphaEntity()
        return AlphaResponse()
    }
}
