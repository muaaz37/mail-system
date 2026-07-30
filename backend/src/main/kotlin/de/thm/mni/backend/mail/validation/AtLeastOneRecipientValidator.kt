package de.thm.mni.backend.mail.validation

import de.thm.mni.backend.mail.dto.MailRequest
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * Validates that a mail request has at least one internal or external recipient.
 */
class AtLeastOneRecipientValidator : ConstraintValidator<AtLeastOneRecipient, MailRequest> {
    /**
     * Accepts null values and lets other validators decide whether the request itself is required.
     */
    override fun isValid(value: MailRequest?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true
        return value.toIds.isNotEmpty() ||
            value.ccIds.isNotEmpty() ||
            value.bccIds.isNotEmpty() ||
            value.externalTo.any { recipient -> recipient.isNotBlank() } ||
            value.externalCc.any { recipient -> recipient.isNotBlank() } ||
            value.externalBcc.any { recipient -> recipient.isNotBlank() }
    }
}
