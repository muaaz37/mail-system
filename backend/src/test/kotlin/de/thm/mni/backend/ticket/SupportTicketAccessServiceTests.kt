package de.thm.mni.backend.ticket

import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mailrecord.MailRecord
import de.thm.mni.backend.mailrecord.MailRecordService
import de.thm.mni.backend.user.User
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SupportTicketAccessServiceTests {
    private val mailRecordService = mock(MailRecordService::class.java)
    private val accessService = SupportTicketAccessService(mailRecordService)

    /**
     * Test that an internal ticket is visible to the recipient but not the sender.
     * This test creates an internal mail with a sender and a recipient, and checks
     * that the recipient can view the ticket while the sender cannot.
     */
    @Test
    fun `internal ticket is visible to recipient but not sender`() {
        val sender = user("sender@example.org")
        val recipient = user("recipient@example.org")
        val mail = internalMail(sender)
        val ticket = ticketWith(mail)

        `when`(mailRecordService.getMailRecordByMailId(mail.id!!))
            .thenReturn(listOf(MailRecord(mail, recipient, MailType.TO)))

        assertTrue(accessService.canView(ticket, recipient))
        assertFalse(accessService.canView(ticket, sender))
    }

    /**
     * Test that an external incoming ticket is visible to team profiles.
     * This test creates an external mail with no sender and checks that a team profile can view the ticket.
     */
    @Test
    fun `external incoming ticket remains visible to team profiles`() {
        val mail = Mail().apply {
            id = UUID.randomUUID()
            status = MailStatus.RECEIVED
            deliveryMode = MailDeliveryMode.EXTERNAL
            sender = null
        }

        assertTrue(accessService.canView(ticketWith(mail), user("support@example.org")))
    }

    /**
     * Creates a test user with the specified email.
     */
    private fun user(email: String) = User("Test", "User", email).apply {
        id = UUID.randomUUID()
    }

    /**
     * Creates a test mail with the specified sender.
     */
    private fun internalMail(sender: User) = Mail(sender, "Internal request", "Please help", mutableListOf()).apply {
        id = UUID.randomUUID()
        status = MailStatus.SENT
        deliveryMode = MailDeliveryMode.INTERNAL
    }

    /**
     * Creates a test support ticket with the specified mail attached.
     */
    private fun ticketWith(mail: Mail) = SupportTicket().apply {
        id = UUID.randomUUID()
        mails = mutableListOf(mail)
    }
}
