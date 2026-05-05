package depromeet.hotsix.obrit.architecture

import org.junit.jupiter.api.Test

class LayerDependencyTest {

    companion object {
        private val importedClasses = ArchitectureRules.importProductionClasses()
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
        ArchitectureRules.cycleRule(allowEmptyShould = true).check(importedClasses)
    }
}
