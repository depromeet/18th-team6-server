package depromeet.hotsix.obrit.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import jakarta.persistence.Embeddable
import kotlin.jvm.JvmInline

object ArchitectureRules {
    private const val BASE_PACKAGE = "depromeet.hotsix.obrit"
    private const val GLOBAL_DOMAIN = "global"
    private const val ADMIN_DOMAIN = "admin"

    private const val CONTROLLER_LAYER = "controller"
    private const val SERVICE_LAYER = "service"
    private const val DTO_LAYER = "dto"
    private const val ENTITY_LAYER = "entity"
    private const val REPOSITORY_LAYER = "repository"

    private val DOMAIN_LAYERS = setOf(
        CONTROLLER_LAYER,
        SERVICE_LAYER,
        DTO_LAYER,
        ENTITY_LAYER,
        REPOSITORY_LAYER,
        "client",
    )

    fun importProductionClasses(): JavaClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages(BASE_PACKAGE)

    fun importProductionClassesExcludingControllers(): JavaClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .withImportOption { !it.contains("/$CONTROLLER_LAYER/") }
        .importPackages(BASE_PACKAGE)

    fun layerRules(importedClasses: JavaClasses, allowEmptyShould: Boolean): List<ArchRule> {
        val domains = discoverDomains(importedClasses)

        return buildList {
            domains.forEach { domain ->
                add(controllerRule(domain, allowEmptyShould))
                add(serviceRule(domain, allowEmptyShould))
            }
            add(entityRule(allowEmptyShould))
            add(dtoRule(allowEmptyShould))
        }
    }

    fun controllerRule(domain: String, allowEmptyShould: Boolean): ArchRule = noClasses()
        .that().resideInAPackage(domainLayerPackage(domain, CONTROLLER_LAYER))
        .should().dependOnClassesThat(
            forbiddenProjectDependencyPredicate(controllerDescription(domain)) { javaClass, location ->
                !isControllerDependencyAllowed(domain, location, javaClass)
            },
        )
        .because(
            "Controller는 같은 도메인의 controller/dto, 같은·다른 도메인의 service, global 패키지, 공유 가능한 enum/value object만 참조할 수 있습니다.",
        )
        .allowEmptyShould(allowEmptyShould)

    fun serviceRule(domain: String, allowEmptyShould: Boolean): ArchRule = noClasses()
        .that().resideInAPackage(domainLayerPackage(domain, SERVICE_LAYER))
        .should().dependOnClassesThat(
            forbiddenProjectDependencyPredicate(serviceDescription(domain)) { javaClass, location ->
                !isServiceDependencyAllowed(domain, location, javaClass)
            },
        )
        .because(
            "Service는 같은 도메인의 service, entity, repository, dto, client와 다른 도메인의 service/repository, global 패키지, 공유 가능한 enum/value object만 참조할 수 있습니다.",
        )
        .allowEmptyShould(allowEmptyShould)

    fun entityRule(allowEmptyShould: Boolean): ArchRule = noClasses()
        .that().resideInAPackage("..entity..")
        .should().dependOnClassesThat(
            forbiddenProjectDependencyPredicate("service/controller/repository 레이어") { _, location ->
                location.layer in setOf(SERVICE_LAYER, CONTROLLER_LAYER, REPOSITORY_LAYER)
            },
        )
        .because("Entity는 service, controller, repository 레이어에 의존하면 안 됩니다.")
        .allowEmptyShould(allowEmptyShould)

    fun dtoRule(allowEmptyShould: Boolean): ArchRule = noClasses()
        .that().resideInAPackage("..dto..")
        .should().dependOnClassesThat(
            forbiddenProjectDependencyPredicate("공유 가능한 enum/value object를 제외한 entity 레이어") { javaClass, location ->
                location.layer == ENTITY_LAYER && !isShareableDomainType(javaClass)
            },
        )
        .because("DTO에서 JPA Entity/일반 도메인 모델을 직접 참조하면 안 됩니다. enum/value object는 공유 타입으로 참조할 수 있습니다.")
        .allowEmptyShould(allowEmptyShould)

    fun cycleRule(allowEmptyShould: Boolean): ArchRule = slices()
        .matching("$BASE_PACKAGE.(*)..")
        .should().beFreeOfCycles()
        .because("도메인 간 순환 의존성은 허용되지 않습니다. (controller 레이어는 제외)")
        .allowEmptyShould(allowEmptyShould)

    private fun discoverDomains(importedClasses: JavaClasses): Set<String> = importedClasses
        .mapNotNullTo(sortedSetOf()) { javaClass ->
            projectLocation(javaClass.packageName)
                ?.takeIf { it.domain != GLOBAL_DOMAIN && it.layer in DOMAIN_LAYERS }
                ?.domain
        }

    private fun domainLayerPackage(domain: String, layer: String): String = "$BASE_PACKAGE.$domain.$layer.."

    private fun controllerDescription(domain: String): String =
        "$domain controller는 같은 도메인의 controller/service/dto, 다른 도메인의 service, global 패키지, 공유 가능한 enum/value object만 참조할 수 있습니다."

    private fun serviceDescription(domain: String): String =
        "$domain service는 같은 도메인의 service/entity/repository/dto/client, 다른 도메인의 service/repository, global 패키지, 공유 가능한 enum/value object만 참조할 수 있습니다."

    private fun forbiddenProjectDependencyPredicate(
        description: String,
        isForbidden: (JavaClass, ProjectLocation) -> Boolean,
    ): DescribedPredicate<JavaClass> = object : DescribedPredicate<JavaClass>(description) {
        override fun test(input: JavaClass): Boolean {
            val location = projectLocation(input.packageName) ?: return false
            return isForbidden(input, location)
        }
    }

    private fun isControllerDependencyAllowed(
        domain: String,
        location: ProjectLocation,
        javaClass: JavaClass,
    ): Boolean = when {
        location.domain == GLOBAL_DOMAIN -> true
        isShareableDomainType(javaClass) -> true
        location.layer == SERVICE_LAYER -> true
        location.domain != domain -> false
        else -> location.layer in setOf(CONTROLLER_LAYER, DTO_LAYER)
    }

    private fun isServiceDependencyAllowed(domain: String, location: ProjectLocation, javaClass: JavaClass): Boolean =
        when {
            location.domain == GLOBAL_DOMAIN -> true
            isShareableDomainType(javaClass) -> true
            domain == ADMIN_DOMAIN -> location.layer in setOf(ENTITY_LAYER, REPOSITORY_LAYER, DTO_LAYER, SERVICE_LAYER)
            location.layer == SERVICE_LAYER -> true
            location.layer == REPOSITORY_LAYER -> true
            location.domain != domain -> false
            else -> location.layer in setOf(ENTITY_LAYER, REPOSITORY_LAYER, DTO_LAYER, "client")
        }

    private fun isShareableDomainType(javaClass: JavaClass): Boolean =
        javaClass.packageName.endsWith(".$ENTITY_LAYER") &&
            (
                javaClass.isEnum ||
                    javaClass.isAnnotatedWith(Embeddable::class.java) ||
                    javaClass.isAnnotatedWith(JvmInline::class.java) ||
                    javaClass.simpleName.endsWith("Value") ||
                    javaClass.simpleName.endsWith("ValueObject") ||
                    javaClass.simpleName.endsWith("Snapshot") ||
                    javaClass.simpleName.endsWith("Projection") ||
                    javaClass.simpleName.endsWith("View")
                )

    private fun projectLocation(packageName: String): ProjectLocation? {
        if (!packageName.startsWith("$BASE_PACKAGE.")) {
            return null
        }

        val relativePackage = packageName.removePrefix("$BASE_PACKAGE.")
        val segments = relativePackage.split('.')

        if (segments.isEmpty()) {
            return null
        }

        return ProjectLocation(
            domain = segments.first(),
            layer = segments.getOrNull(1),
        )
    }

    private data class ProjectLocation(val domain: String, val layer: String?)
}
