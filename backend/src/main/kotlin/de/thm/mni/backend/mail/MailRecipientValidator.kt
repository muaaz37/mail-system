package de.thm.mni.backend.mail

import de.thm.mni.backend.error.InvalidMailRequestException
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.user.User
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Validates whether recipients match the selected internal or external delivery mode.
 */
@Component
class MailRecipientValidator {
    /**
     * Checks recipient consistency before a mail is created or updated.
     */
    fun validate(
        deliveryMode: MailDeliveryMode,
        internalRecipientIds: List<UUID>,
        externalRecipients: List<String>,
        sender: User
    ) {
        val hasInternalRecipients = internalRecipientIds.isNotEmpty()
        val hasExternalRecipients = externalRecipients.any { recipient -> recipient.isNotBlank() }

        when (deliveryMode) {
            MailDeliveryMode.INTERNAL -> validateInternalRecipients(
                hasInternalRecipients,
                hasExternalRecipients,
                sender,
                internalRecipientIds
            )
            MailDeliveryMode.EXTERNAL -> validateExternalRecipients(
                hasInternalRecipients,
                hasExternalRecipients,
                externalRecipients
            )
        }
    }

    /**
     * Allows only existing team profiles as recipients for internal delivery.
     */
    private fun validateInternalRecipients(
        hasInternalRecipients: Boolean,
        hasExternalRecipients: Boolean,
        sender: User,
        internalRecipientIds: List<UUID>
    ) {
        if (!hasInternalRecipients || hasExternalRecipients) {
            throw InvalidMailRequestException("Internal mails must use existing team profiles only.")
        }
        if (internalRecipientIds.contains(sender.id)) {
            throw InvalidMailRequestException("You cannot send an internal mail to yourself.")
        }
    }

    /**
     * Allows only valid email addresses as recipients for external delivery.
     */
    private fun validateExternalRecipients(
        hasInternalRecipients: Boolean,
        hasExternalRecipients: Boolean,
        externalRecipients: List<String>
    ) {
        if (!hasExternalRecipients || hasInternalRecipients) {
            throw InvalidMailRequestException("External mails must use email addresses only.")
        }
        externalRecipients
            .filter { recipient -> recipient.isNotBlank() }
            .forEach { recipient -> validateEmail(recipient) }
    }

    private fun validateEmail(email: String) {
        try {
            InternetAddress(email, true).validate()
        } catch (ex: AddressException) {
            throw InvalidMailRequestException("Invalid email address: ", ex)
        }
    }
}
