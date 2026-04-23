package de.thm.mni.backend.mail.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [AtLeastOneRecipientValidator::class])
annotation class AtLeastOneRecipient(
    val message: String = "At least one recipient (to, cc or bcc) must be provided",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
