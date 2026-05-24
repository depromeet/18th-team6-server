package depromeet.hotsix.obrit.global.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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

    @Bean
    fun adminSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers("/admin/**").authenticated()
                    .anyRequest().permitAll()
            }
            .httpBasic(Customizer.withDefaults())
            .formLogin(Customizer.withDefaults())
            .csrf(Customizer.withDefaults())

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
}
