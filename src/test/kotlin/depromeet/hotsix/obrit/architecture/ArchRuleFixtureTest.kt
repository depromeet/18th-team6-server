package depromeet.hotsix.obrit.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import depromeet.hotsix.obrit.alpha.controller.AllowedAlphaController
import depromeet.hotsix.obrit.alpha.controller.InvalidAlphaEntityController
import depromeet.hotsix.obrit.alpha.controller.InvalidBetaServiceController
import depromeet.hotsix.obrit.alpha.dto.AlphaResponse
import depromeet.hotsix.obrit.alpha.dto.InvalidAlphaEntityResponse
import depromeet.hotsix.obrit.alpha.entity.AlphaEntity
import depromeet.hotsix.obrit.alpha.repository.AlphaRepository
import depromeet.hotsix.obrit.alpha.service.AllowedAlphaService
import depromeet.hotsix.obrit.alpha.service.InvalidBetaControllerService
import depromeet.hotsix.obrit.alpha.service.InvalidBetaDtoService
import depromeet.hotsix.obrit.alpha.service.InvalidBetaEntityService
import depromeet.hotsix.obrit.alpha.service.InvalidBetaRepositoryService
import depromeet.hotsix.obrit.beta.controller.BetaController
import depromeet.hotsix.obrit.beta.dto.BetaResponse
import depromeet.hotsix.obrit.beta.entity.BetaEntity
import depromeet.hotsix.obrit.beta.repository.BetaRepository
import depromeet.hotsix.obrit.beta.service.BetaService
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ArchRuleFixtureTest {

    @Test
    fun `controller는 같은 도메인의 service와 dto를 참조할 수 있다`() {
        val classes = importClasses(
            AllowedAlphaController::class.java,
            AllowedAlphaService::class.java,
            AlphaResponse::class.java,
            AlphaEntity::class.java,
            AlphaRepository::class.java,
            BetaService::class.java,
        )

        assertDoesNotThrow {
            ArchitectureRules.controllerRule("alpha", allowEmptyShould = false).check(classes)
        }
    }

    @Test
    fun `controller는 같은 도메인의 entity를 참조하면 안 된다`() {
        val classes = importClasses(
            InvalidAlphaEntityController::class.java,
            AlphaEntity::class.java,
        )

        assertThrows(AssertionError::class.java) {
            ArchitectureRules.controllerRule("alpha", allowEmptyShould = false).check(classes)
        }
    }

    @Test
    fun `controller는 다른 도메인의 service를 참조하면 안 된다`() {
        val classes = importClasses(
            InvalidBetaServiceController::class.java,
            BetaService::class.java,
        )

        assertThrows(AssertionError::class.java) {
            ArchitectureRules.controllerRule("alpha", allowEmptyShould = false).check(classes)
        }
    }

    @Test
    fun `service는 같은 도메인의 entity repository dto와 다른 도메인의 service를 참조할 수 있다`() {
        val classes = importClasses(
            AllowedAlphaService::class.java,
            AlphaResponse::class.java,
            AlphaEntity::class.java,
            AlphaRepository::class.java,
            BetaService::class.java,
        )

        assertDoesNotThrow {
            ArchitectureRules.serviceRule("alpha", allowEmptyShould = false).check(classes)
        }
    }

    @Test
    fun `service는 다른 도메인의 entity를 참조하면 안 된다`() {
        val classes = importClasses(
            InvalidBetaEntityService::class.java,
            BetaEntity::class.java,
        )

        assertThrows(AssertionError::class.java) {
            ArchitectureRules.serviceRule("alpha", allowEmptyShould = false).check(classes)
        }
    }

    @Test
    fun `service는 다른 도메인의 dto를 참조하면 안 된다`() {
        val classes = importClasses(
            InvalidBetaDtoService::class.java,
            BetaResponse::class.java,
        )

        assertThrows(AssertionError::class.java) {
            ArchitectureRules.serviceRule("alpha", allowEmptyShould = false).check(classes)
        }
    }

    @Test
    fun `service는 다른 도메인의 repository를 참조하면 안 된다`() {
        val classes = importClasses(
            InvalidBetaRepositoryService::class.java,
            BetaRepository::class.java,
        )

        assertThrows(AssertionError::class.java) {
            ArchitectureRules.serviceRule("alpha", allowEmptyShould = false).check(classes)
        }
    }

    @Test
    fun `service는 다른 도메인의 controller를 참조하면 안 된다`() {
        val classes = importClasses(
            InvalidBetaControllerService::class.java,
            BetaController::class.java,
        )

        assertThrows(AssertionError::class.java) {
            ArchitectureRules.serviceRule("alpha", allowEmptyShould = false).check(classes)
        }
    }

    @Test
    fun `dto는 entity를 직접 참조하면 안 된다`() {
        val classes = importClasses(
            InvalidAlphaEntityResponse::class.java,
            AlphaEntity::class.java,
        )

        assertThrows(AssertionError::class.java) {
            ArchitectureRules.dtoRule(allowEmptyShould = false).check(classes)
        }
    }

    private fun importClasses(vararg classes: Class<*>): JavaClasses = ClassFileImporter().importClasses(*classes)
}
