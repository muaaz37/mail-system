package de.thm.mni.backend.mail.validation

import de.thm.mni.backend.error.InvalidMailRequestException
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.user.User
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertFailsWith

class MailRecipientValidatorTests {
    private val validator = MailRecipientValidator()
    private val sender = User().apply { id = UUID.randomUUID() }

    @Test
    fun `valid internal recipient is accepted`() {
        validator.validate(
            MailDeliveryMode.INTERNAL,
            listOf(UUID.randomUUID()),
            emptyList(),
            sender
        )
    }

    @Test
    fun `internal mail addressed to sender is rejected`() {
        assertFailsWith<InvalidMailRequestException> {
            validator.validate(
                MailDeliveryMode.INTERNAL,
                listOf(requireNotNull(sender.id)),
                emptyList(),
                sender
            )
        }
    }

    @Test
    fun `valid external recipient is accepted`() {
        validator.validate(
            MailDeliveryMode.EXTERNAL,
            emptyList(),
            listOf("customer@example.org"),
            sender
        )
    }

    @Test
    fun `invalid external recipient is rejected`() {
        assertFailsWith<InvalidMailRequestException> {
            validator.validate(
                MailDeliveryMode.EXTERNAL,
                emptyList(),
                listOf("invalid-address"),
                sender
            )
        }
    }
}
