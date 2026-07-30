package de.thm.mni.backend.security

import com.auth0.jwt.exceptions.JWTVerificationException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Invalid browser tokens fail here before Spring Security reaches controller exception handling.
 */
@Component
class AuthFilter(private val jwtService: JwtService) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            JwtAuthenticationFactory.create(request, jwtService)?.let {
                SecurityContextHolder.getContext().authentication = it
            }
        } catch (_: JWTVerificationException) {
            rejectInvalidToken(response)
            return
        } catch (_: IllegalArgumentException) {
            rejectInvalidToken(response)
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun rejectInvalidToken(response: HttpServletResponse) {
        SecurityContextHolder.clearContext()
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(INVALID_SESSION_RESPONSE_BODY)
    }

    private companion object {
        const val INVALID_SESSION_RESPONSE_BODY = """{"status":401,"message":"Session expired. Please log in again."}"""
    }
}
