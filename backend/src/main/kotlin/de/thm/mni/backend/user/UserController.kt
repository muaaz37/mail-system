package de.thm.mni.backend.user

import de.thm.mni.backend.error.ResourceAlreadyExistsException
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.user.dto.UserDTO
import de.thm.mni.backend.user.dto.UserUpdate
import de.thm.mni.backend.user.dto.toDTO
import de.thm.mni.backend.openapi.BearerAuthenticated
import de.thm.mni.backend.openapi.DefaultApiErrors
import de.thm.mni.backend.openapi.BadRequestApiResponse
import de.thm.mni.backend.openapi.ConflictApiResponse
import de.thm.mni.backend.openapi.NotFoundApiResponse
import de.thm.mni.backend.security.CurrentUserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Provides user lookup and self-service profile management endpoints.
 */
@Tag(name = "User", description = "Manage registered application users.")
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
     * Returns all registered users for internal recipient selection.
     */
    @GetMapping
    @Operation(
        operationId = "getUsers",
        summary = "List users",
        description = "Returns all registered users available for internal recipient selection."
    )
    @ApiResponse(responseCode = "200", description = "Users returned successfully.")
    fun getAllUsers(): List<UserDTO> {
        return userService.getAllUsers().map { user -> user.toDTO() }
    }

    /**
     * Returns one user by identifier when it exists.
     */
    @GetMapping("/{id}")
    @Operation(
        operationId = "getUserById",
        summary = "Get a user",
        description = "Returns a user's public profile by identifier."
    )
    @ApiResponse(responseCode = "200", description = "User returned successfully.")
    @NotFoundApiResponse
    fun getUserById(
        @Parameter(description = "Local user identifier returned by `GET /api/users` or `GET /api/users/me`.")
        @PathVariable id: UUID
    ): UserDTO {
        return userService.getUserById(id)?.toDTO() ?: throw ResourceNotFoundException("User not found")
    }

    /**
     * Updates the authenticated user's own profile data.
     */
    @PutMapping("/{id}")
    @Operation(
        operationId = "updateUser",
        summary = "Update a user profile",
        description = "Updates the authenticated user's own public profile."
    )
    @ApiResponse(responseCode = "200", description = "User profile updated successfully.")
    @BadRequestApiResponse
    @NotFoundApiResponse
    @ConflictApiResponse
    fun updateUser(
        @Parameter(description = "Local identifier returned by `GET /api/users/me`.")
        @PathVariable id: UUID,
        @Valid @RequestBody userData: UserUpdate,
        @AuthenticationPrincipal jwt: Jwt
    ): UserDTO? {
        val existingUser = authorizedUser(id, jwt)
        ensureEmailAvailable(userData.email, existingUser.id!!)

        val updatedUser = User(
            firstName = userData.firstName,
            lastName = userData.lastName,
            email = userData.email,
            identitySubject = existingUser.identitySubject
        )

        return userService.updateUser(id, updatedUser).toDTO()
    }

    /**
     * Deletes the authenticated user's own account.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        operationId = "deleteUser",
        summary = "Delete a user account",
        description = "Deletes the authenticated user's own account."
    )
    @ApiResponse(responseCode = "204", description = "User account deleted successfully.")
    @NotFoundApiResponse
    fun deleteUser(
        @Parameter(description = "Local identifier returned by `GET /api/users/me`.")
        @PathVariable id: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ) {
        authorizedUser(id, jwt)
        userService.deleteUser(id)
    }

    /**
     * Loads the requested user only when it belongs to the authenticated principal.
     */
    private fun authorizedUser(id: UUID, jwt: Jwt): User {
        val existingUser = userService.getUserById(id) ?: throw ResourceNotFoundException("User not found")
        if (existingUser.identitySubject != jwt.subject) {
            throw ResourceNotFoundException("User not found")
        }
        return existingUser
    }

    /**
     * Prevents changing a profile to an email address used by another account.
     */
    private fun ensureEmailAvailable(email: String, currentUserId: UUID) {
        val userWithExistingEmail = userService.getUserByEmail(email)
        if (userWithExistingEmail != null && userWithExistingEmail.id != currentUserId) {
            throw ResourceAlreadyExistsException("Email is already in use by another user")
        }
    }
}
