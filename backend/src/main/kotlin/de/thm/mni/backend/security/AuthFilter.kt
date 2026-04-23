package de.thm.mni.backend.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter


@Component
class AuthFilter(private val jwtService: JwtService): OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        JwtAuthenticationFactory.create(request, jwtService)?.let {
            SecurityContextHolder.getContext().authentication = it
        }
        filterChain.doFilter(request, response)
    }
}