package depromeet.hotsix.obrit.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(
    @Value("\${spring.application.version:0.0.1}")
    private val applicationVersion: String,
) {

    // Swagger UI에서 Basic Auth로 보호된 API(예: /admin/**)를 호출할 수 있게
    // "basicAuth" 보안 스키마를 등록한다. 컨트롤러에 @SecurityRequirement(name="basicAuth")
    // 를 붙이면 우상단 Authorize 버튼이 활성화된다.
    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Obrit API")
                .version(applicationVersion)
                .description("Inventory, category, and replacement-cycle API for Obrit."),
        )
        .components(
            Components().addSecuritySchemes(
                "basicAuth",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("basic"),
            ),
        )
}
