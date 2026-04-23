package de.thm.mni.backend.user


import de.thm.mni.backend.error.ResourceAlreadyExistsException
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.user.dto.UserDTO
import de.thm.mni.backend.user.dto.UserUpdate
import de.thm.mni.backend.user.dto.toDTO
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService, private val passwordEncoder: PasswordEncoder) {


    @GetMapping
    fun getAllUsers(): List<UserDTO> {
        return userService.getAllUsers().map { it -> it.toDTO() }
    }

    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: UUID): UserDTO? {
        return userService.getUserById(id)?.toDTO()
    }

    @PutMapping("/{id}")
    fun updateUser(@PathVariable id: UUID,
                   @Valid @RequestBody userData: UserUpdate,
                   @AuthenticationPrincipal user: UserDetails): UserDTO? {
        val existingUser = userService.getUserById(id) ?: throw ResourceNotFoundException("User not found")

        if(existingUser.id.toString() != user.username) {
            throw ResourceNotFoundException("User not found")
        }

        val userWithExistingEmail = userService.getUserByEmail(userData.email)

        if ( userWithExistingEmail != null && userWithExistingEmail.id != existingUser.id ) {
            throw ResourceAlreadyExistsException("Email is already in use by another user")
        }

       val updatedUser = User(
            firstName = userData.firstName,
            lastName = userData.lastName,
            email = userData.email,
            password = existingUser.password
        )
        updatedUser.id = existingUser.id

        return userService.updateUser(id, updatedUser).toDTO()
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(@PathVariable id: UUID, @AuthenticationPrincipal user: UserDetails) {
        val existingUser = userService.getUserById(id) ?: throw ResourceNotFoundException("User not found")
        if(existingUser.id.toString() != user.username) {
            throw ResourceNotFoundException("User not found")
        }
        userService.deleteUser(id)
    }

}