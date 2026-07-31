package de.thm.mni.backend.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID


/**
 * Local application profile linked to an identity managed by Keycloak.
 *
 * Authentication credentials are stored exclusively by the identity provider.
 */
@Entity
@Table(name = "users")
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id : UUID? = null

    @Column(name = "first_name")
    var firstName: String = ""

    @Column(name = "last_name")
    var lastName: String = ""

    @Column(name = "email", unique = true)
    var email: String = ""

    /**
     * Stable OpenID Connect subject assigned by Keycloak.
     */
    @Column(name = "identity_subject", unique = true)
    var identitySubject: String? = null


    constructor()

    /**
     * Creates a new user with the given profile data.
     * @param firstName First name of the user.
     * @param lastName Last name of the user.
     * @param email Email address of the user.
     * @param identitySubject Stable OpenID Connect subject assigned by Keycloak.
     */
    constructor(
        firstName: String,
        lastName: String,
        email: String,
        identitySubject: String? = null
    ) {
        this.firstName = firstName
        this.lastName = lastName
        this.email = email
        this.identitySubject = identitySubject
    }

}
