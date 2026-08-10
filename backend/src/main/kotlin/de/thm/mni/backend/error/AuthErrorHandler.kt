package de.thm.mni.backend.error

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/**
 * Converts Spring Security authentication failures into the public JSON error contract.
 */
@Component
class AuthErrorHandler : AuthenticationEntryPoint {
    /**
     * Sends a sanitized 401 response instead of exposing framework-specific authentication errors.
     */
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(UNAUTHORIZED_RESPONSE_BODY)
    }

    /**
     * Shared response body for unauthenticated API calls.
     */
    private companion object {
        const val UNAUTHORIZED_RESPONSE_BODY = """{"status":401,"message":"Authentication required. Please log in."}"""
    }
}
