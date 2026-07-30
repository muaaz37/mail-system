package de.thm.mni.backend.security

import de.thm.mni.backend.error.AuthErrorHandler
import de.thm.mni.backend.util.SaltPepperPasswordEncoder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration

/**
 * Configures password hashing, JWT filtering and HTTP security rules.
 */
@Configuration
class SecurityConfig(@Value("\${app.secret}") private val pepper: String) {
    /**
     * Provides the password encoder with the configured application pepper.
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = SaltPepperPasswordEncoder(pepper)

    /**
     * Disables Spring Boot's generated default user; this API authenticates only via custom JWT handling.
     */
    @Bean
    fun userDetailsService(): UserDetailsService =
        UserDetailsService { throw UsernameNotFoundException("Default login users are not configured.") }

    /**
     * Builds the stateless API security filter chain.
     */
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        authFilter: AuthFilter,
        authErrorHandler: AuthErrorHandler
    ): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .cors {
                it.configurationSource {
                    CorsConfiguration().apply {
                        allowedOriginPatterns = listOf("*")
                        allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        allowedHeaders = listOf("*")
                        allowCredentials = true
                        maxAge = CORS_MAX_AGE_SECONDS
                    }
                }
            }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/api/register",
                    "/api/login",
                    "/api/v3/api-docs",
                    "/api/v3/api-docs/**",
                    "/api/swagger-ui",
                    "/api/swagger-ui/**"
                ).permitAll()
                it.anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(authErrorHandler)
            }
            .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter::class.java)
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .build()

    private companion object {
        const val CORS_MAX_AGE_SECONDS = 3_600L
    }
}
