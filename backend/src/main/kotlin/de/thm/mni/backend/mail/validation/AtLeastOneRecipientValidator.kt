package de.thm.mni.backend.mail.validation

import de.thm.mni.backend.mail.dto.MailRequest
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class AtLeastOneRecipientValidator : ConstraintValidator<AtLeastOneRecipient, MailRequest> {
    override fun isValid(value: MailRequest?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true
        return value.toIds.isNotEmpty() || value.ccIds.isNotEmpty() || value.bccIds.isNotEmpty()
    }
}