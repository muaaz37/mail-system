package de.thm.mni.backend.security

import de.thm.mni.backend.error.AuthenticatedUserNotFoundException
import de.thm.mni.backend.user.User
import de.thm.mni.backend.user.UserService
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service

/**
 * Links a verified Keycloak identity to local application profile data.
 *
 * Authentication credentials remain exclusively in Keycloak.
 */
@Service
class CurrentUserService(
    private val userService: UserService
) {

    /**
     * Finds or creates the local profile associated with a Keycloak identity.
     * @param jwt The JWT token containing the identity subject.
     * @return The local profile associated with the identity.
     * @throws AuthenticatedUserNotFoundException If the identity subject is not found.
     */
    fun resolve(jwt: Jwt): User {
        val subject = jwt.subject
            ?: throw AuthenticatedUserNotFoundException(
                "The authenticated identity does not contain a subject identifier."
            )

        userService.getUserByIdentitySubject(subject)?.let {
            return synchronizeProfile(it, jwt)
        }

        val email = jwt.getClaimAsString("email")
            ?: throw AuthenticatedUserNotFoundException(
                "The authenticated identity does not contain an email address."
            )

        val user = userService.getUserByEmail(email) ?: User(
            firstName = getClaim(jwt, "given_name"),
            lastName = getClaim(jwt, "family_name"),
            email = email
        )

        user.identitySubject = subject
        return userService.createUser(user)
    }

    /**
     * Synchronizes profile attributes managed by Keycloak.
     */
    private fun synchronizeProfile(user: User, jwt: Jwt): User {
        val email = jwt.getClaimAsString("email") ?: user.email
        val firstName = getClaim(jwt, "given_name").ifBlank { user.firstName }
        val lastName = getClaim(jwt, "family_name").ifBlank { user.lastName }

        if (
            user.email != email ||
            user.firstName != firstName ||
            user.lastName != lastName
        ) {
            user.email = email
            user.firstName = firstName
            user.lastName = lastName
            return userService.createUser(user)
        }

        return user
    }

    /**
     * Returns a claim value from the JWT or an empty string if the claim is not present.
     */
    private fun getClaim(jwt: Jwt, name: String): String {
        return jwt.getClaimAsString(name).orEmpty()
    }
}