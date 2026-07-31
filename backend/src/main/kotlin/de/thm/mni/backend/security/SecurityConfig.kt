package de.thm.mni.backend.security

import de.thm.mni.backend.error.AuthErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration


/**
 * Configures the API as a stateless OAuth 2.0 resource server.
 * Keycloak authenticates users, while Spring Security validates access tokens.
 */
@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        authErrorHandler: AuthErrorHandler
    ): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .cors {
                it.configurationSource {
                    CorsConfiguration().apply {
                        allowedOrigins = listOf(
                            "http://localhost:8081",
                            "http://localhost:4200"
                        )
                        allowedMethods = listOf(
                            "GET",
                            "POST",
                            "PUT",
                            "DELETE",
                            "OPTIONS"
                        )
                        allowedHeaders = listOf("*")
                        allowCredentials = true
                        maxAge = CORS_MAX_AGE_SECONDS
                    }
                }
            }
            // Configure session management for stateless operation
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/api/v3/api-docs",
                    "/api/v3/api-docs/**",
                    "/api/swagger-ui",
                    "/api/swagger-ui/**",
                    "/api/swagger-ui.html"
                ).permitAll()

                it.anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(authErrorHandler)
            }
            .oauth2ResourceServer {
                it.jwt { }
                it.authenticationEntryPoint(authErrorHandler)
            }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .build()

    private companion object {
        const val CORS_MAX_AGE_SECONDS = 3_600L
    }
}