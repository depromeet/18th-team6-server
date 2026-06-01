package depromeet.hotsix.obrit.global.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(private val accessLogInterceptor: AccessLogInterceptor) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(accessLogInterceptor)
            .excludePathPatterns("/actuator/**", "/admin/**")
    }
}
