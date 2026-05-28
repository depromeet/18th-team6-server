package depromeet.hotsix.obrit.global.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
class AdminSecurityConfig {

    // /admin/** 요청에 대해 적용하는 시큐리티 필터 체인
    @Bean
    @Order(1)
    fun adminSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/admin/**", "/login", "/logout")
            .authorizeHttpRequests { requests ->
                requests.anyRequest().authenticated()
            }
            .formLogin { form -> form.defaultSuccessUrl("/admin", true) }
            .csrf(Customizer.withDefaults())

        return http.build()
    }

    // 일반 API 접근 시 사용하는 시큐리티 필터 체인. 화이트리스트만 허용
    @Bean
    @Order(2)
    fun apiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers(*API_WHITE_LIST).permitAll()
                    .anyRequest().authenticated()
            }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }

        return http.build()
    }

    @Bean
    fun adminUserDetailsService(
        @Value("\${obrit.admin.username}") adminUsername: String,
        @Value("\${obrit.admin.password}") adminPassword: String,
        passwordEncoder: PasswordEncoder,
    ): UserDetailsService {
        val adminUser = User.withUsername(adminUsername)
            .password(passwordEncoder.encode(adminPassword))
            .roles("ADMIN")
            .build()

        return InMemoryUserDetailsManager(adminUser)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    companion object {
        private val API_WHITE_LIST = arrayOf(
            "/users", "/users/**",
            "/items", "/items/**",
            "/categories", "/categories/**",
            "/home", "/home/**",
            "/swagger-ui/**", "/swagger-ui.html",
            "/v3/api-docs/**",
        )
    }
}
