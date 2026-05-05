package depromeet.hotsix.obrit.global.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(
    @Value("\${spring.application.version:0.0.1}")
    private val applicationVersion: String,
) {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Obrit API")
                .version(applicationVersion)
                .description("Inventory, category, and replacement-cycle API for Obrit."),
        )
}
