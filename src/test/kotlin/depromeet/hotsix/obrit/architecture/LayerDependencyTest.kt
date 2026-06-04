package depromeet.hotsix.obrit.architecture

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LayerDependencyTest {

    companion object {
        private const val BASE_PACKAGE = "depromeet.hotsix.obrit"

        private val importedClasses = ArchitectureRules.importProductionClasses()
        private val classesExcludingControllers = ArchitectureRules.importProductionClassesExcludingControllers()
    }

    @Test
    fun `프로덕션 도메인 클래스는 레이어 패키지 안에 있어야 한다`() {
        val layerPattern = Regex(
            """^$BASE_PACKAGE\.[^.]+\.(controller|service|dto|entity|repository)(\..*)?$""",
        )
        val violations = importedClasses
            .filter { it.packageName.startsWith("$BASE_PACKAGE.") }
            .filterNot { it.packageName.startsWith("$BASE_PACKAGE.global.") }
            .filterNot { it.packageName == BASE_PACKAGE }
            .filterNot { layerPattern.matches(it.packageName) }
            .map { "${it.name} (${it.packageName})" }
            .sorted()

        assertTrue(
            violations.isEmpty(),
            "Production domain classes must live under controller/service/dto/entity/repository packages: $violations",
        )
    }

    @Test
    fun `레이어 의존성 규칙을 준수해야 한다`() {
        ArchitectureRules.layerRules(importedClasses, allowEmptyShould = true)
            .forEach { rule ->
                rule.check(importedClasses)
            }
    }

    @Test
    fun `순환 의존성이 없어야 한다`() {
        ArchitectureRules.cycleRule(allowEmptyShould = true).check(classesExcludingControllers)
    }
}
