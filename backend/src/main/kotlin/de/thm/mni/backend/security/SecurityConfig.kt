package de.thm.mni.backend.security

import de.thm.mni.backend.error.AuthErrorHandler
import de.thm.mni.backend.util.SaltPepperPasswordEncoder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration


@Configuration
class SecurityConfig(@Value("\${app.secret}") private val pepper: String) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = SaltPepperPasswordEncoder(pepper)

    @Bean
    fun securityFilterChain(http: HttpSecurity, authFilter: AuthFilter): SecurityFilterChain =
        http
            .csrf { it.disable() } // Explicitly disable CSRF protection for stateless APIs
            .cors {
                it.configurationSource { request ->
                        val config = CorsConfiguration()
                        config.allowedOriginPatterns = listOf("*")
                        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        config.allowedHeaders = listOf("*")
                        config.allowCredentials = true
                        config.maxAge = 3600L
                        config
                    }
            }
            .authorizeHttpRequests {
                it.requestMatchers("/api/register", "/api/login").permitAll()
                it.anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(AuthErrorHandler())
            }
            .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter::class.java)
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .build()
}