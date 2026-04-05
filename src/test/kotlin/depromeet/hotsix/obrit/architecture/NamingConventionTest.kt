package depromeet.hotsix.obrit.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.Test
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController

class NamingConventionTest {

    companion object {
        private val importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("depromeet.hotsix.obrit")
    }

    @Test
    fun `RestController 어노테이션 클래스는 Controller로 끝나야 한다`() {
        classes()
            .that().areAnnotatedWith(RestController::class.java)
            .should().haveSimpleNameEndingWith("Controller")
            .because("@RestController 클래스는 *Controller 네이밍을 따라야 합니다.")
            .check(importedClasses)
    }

    @Test
    fun `Service 어노테이션 클래스는 Service로 끝나야 한다`() {
        classes()
            .that().areAnnotatedWith(Service::class.java)
            .should().haveSimpleNameEndingWith("Service")
            .because("@Service 클래스는 *Service 네이밍을 따라야 합니다.")
            .check(importedClasses)
    }

    @Test
    fun `controller 패키지의 클래스는 Controller로 끝나야 한다`() {
        classes()
            .that().resideInAPackage("..controller..")
            .should().haveSimpleNameEndingWith("Controller")
            .because("controller 패키지의 클래스는 *Controller 네이밍을 따라야 합니다.")
            .check(importedClasses)
    }

    @Test
    fun `service 패키지의 클래스는 Service로 끝나야 한다`() {
        classes()
            .that().resideInAPackage("..service..")
            .should().haveSimpleNameEndingWith("Service")
            .because("service 패키지의 클래스는 *Service 네이밍을 따라야 합니다.")
            .check(importedClasses)
    }

    @Test
    fun `repository 패키지의 클래스는 Repository로 끝나야 한다`() {
        classes()
            .that().resideInAPackage("..repository..")
            .should().haveSimpleNameEndingWith("Repository")
            .orShould().haveSimpleNameEndingWith("RepositoryImpl")
            .because("repository 패키지의 클래스는 *Repository 또는 *RepositoryImpl 네이밍을 따라야 합니다.")
            .check(importedClasses)
    }
}
