package de.thm.mni.backend.user

import de.thm.mni.backend.error.ResourceAlreadyExistsException
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.user.dto.UserDTO
import de.thm.mni.backend.user.dto.UserUpdate
import de.thm.mni.backend.user.dto.toDTO
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
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
@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {
    /**
     * Returns all registered users for internal recipient selection.
     */
    @GetMapping
    fun getAllUsers(): List<UserDTO> {
        return userService.getAllUsers().map { user -> user.toDTO() }
    }

    /**
     * Returns one user by identifier when it exists.
     */
    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: UUID): UserDTO? {
        return userService.getUserById(id)?.toDTO()
    }

    /**
     * Updates the authenticated user's own profile data.
     */
    @PutMapping("/{id}")
    fun updateUser(
        @PathVariable id: UUID,
        @Valid @RequestBody userData: UserUpdate,
        @AuthenticationPrincipal userDetails: UserDetails
    ): UserDTO? {
        val existingUser = authorizedUser(id, userDetails)
        ensureEmailAvailable(userData.email, existingUser.id!!)

        val updatedUser = User(
            firstName = userData.firstName,
            lastName = userData.lastName,
            email = userData.email,
            password = existingUser.password
        )

        return userService.updateUser(id, updatedUser).toDTO()
    }

    /**
     * Deletes the authenticated user's own account.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(@PathVariable id: UUID, @AuthenticationPrincipal userDetails: UserDetails) {
        authorizedUser(id, userDetails)
        userService.deleteUser(id)
    }

    /**
     * Loads the requested user only when it belongs to the authenticated principal.
     */
    private fun authorizedUser(id: UUID, userDetails: UserDetails): User {
        val existingUser = userService.getUserById(id) ?: throw ResourceNotFoundException("User not found")
        if (existingUser.id.toString() != userDetails.username) {
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
