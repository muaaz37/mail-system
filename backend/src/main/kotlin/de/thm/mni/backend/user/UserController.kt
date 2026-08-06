package de.thm.mni.backend.user

import de.thm.mni.backend.user.dto.UserDTO
import de.thm.mni.backend.user.dto.toDTO
import de.thm.mni.backend.openapi.BearerAuthenticated
import de.thm.mni.backend.openapi.DefaultApiErrors
import de.thm.mni.backend.security.CurrentUserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Exposes local profile data synchronized from Keycloak identities.
 */
@Tag(name = "User", description = "View application user profiles synchronized from Keycloak.")
@BearerAuthenticated
@DefaultApiErrors
@RestController
@RequestMapping("/api/users", produces = [MediaType.APPLICATION_JSON_VALUE])
class UserController(
    private val userService: UserService,
    private val currentUserService: CurrentUserService
) {
    /**
     * Returns the local profile linked to the authenticated Keycloak identity.
     */
    @GetMapping("/me")
    @Operation(
        operationId = "getCurrentUser",
        summary = "Get current user",
        description = "Returns the local profile linked to the authenticated OpenID Connect identity."
    )
    @ApiResponse(responseCode = "200", description = "Current user returned successfully.")
    fun getCurrentUser(@AuthenticationPrincipal jwt: Jwt): UserDTO {
        return currentUserService.resolve(jwt).toDTO()
    }

    /**
     * Returns all local profiles for internal recipient selection.
     */
    @GetMapping
    @Operation(
        operationId = "getUsers",
        summary = "List user profiles",
        description = "Returns local profiles available for internal recipient selection."
    )
    @ApiResponse(responseCode = "200", description = "User profiles returned successfully.")
    fun getAllUsers(): List<UserDTO> {
        return userService.getAllUsers().map { user -> user.toDTO() }
    }
}
