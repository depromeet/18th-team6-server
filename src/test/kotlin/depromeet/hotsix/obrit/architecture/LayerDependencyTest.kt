package depromeet.hotsix.obrit.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.Test

class LayerDependencyTest {

    companion object {
        private val importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("depromeet.hotsix.obrit")
    }

    @Test
    fun `controller는 같은 도메인의 service와 dto만 의존해야 한다`() {
        noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..entity..")
            .because("Controller는 Entity에 직접 의존하면 안 됩니다. DTO를 사용하세요.")
            .allowEmptyShould(true)
            .check(importedClasses)
    }

    @Test
    fun `controller는 같은 도메인의 repository에 의존하면 안 된다`() {
        noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .because("Controller는 Repository에 직접 접근하면 안 됩니다. Service를 통해 접근하세요.")
            .allowEmptyShould(true)
            .check(importedClasses)
    }

    @Test
    fun `entity는 service, controller, repository에 의존하면 안 된다`() {
        noClasses()
            .that().resideInAPackage("..entity..")
            .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..", "..repository..")
            .because("Entity는 순수 도메인 모델이어야 합니다. 외부 레이어에 의존하면 안 됩니다.")
            .allowEmptyShould(true)
            .check(importedClasses)
    }

    @Test
    fun `dto는 entity에 직접 의존하면 안 된다`() {
        noClasses()
            .that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().resideInAPackage("..entity..")
            .because("DTO에서 Entity를 직접 참조하면 안 됩니다. 변환은 Service에서 수행하세요.")
            .allowEmptyShould(true)
            .check(importedClasses)
    }

    @Test
    fun `순환 의존성이 없어야 한다`() {
        slices()
            .matching("depromeet.hotsix.obrit.(*)..")
            .should().beFreeOfCycles()
            .because("도메인 간 순환 의존성은 허용되지 않습니다.")
            .allowEmptyShould(true)
            .check(importedClasses)
    }
}
