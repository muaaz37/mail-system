package de.thm.mni.backend.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource


object JwtAuthenticationFactory{

    fun create(request: HttpServletRequest, jwtService: JwtService): Authentication? {
        val token = request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?: return null

        val jwt = jwtService.validate(token)

        val userDetails = User.builder()
            .username(jwt.subject)
            .password("")
            .roles( "USER")
            .build()

        return UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities).apply {
            details = WebAuthenticationDetailsSource().buildDetails(request)
        }
    }

}