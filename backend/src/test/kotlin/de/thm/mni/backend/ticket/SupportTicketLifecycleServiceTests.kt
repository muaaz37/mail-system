package de.thm.mni.backend.ticket

import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.MailRepository
import de.thm.mni.backend.mail.SupportTicketService
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.enums.MailStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class SupportTicketLifecycleServiceTests {
    private val ticketRepository = mock(SupportTicketRepository::class.java)
    private val mailRepository = mock(MailRepository::class.java)
    private val readStateService = mock(SupportTicketReadStateService::class.java)
    private val supportTicketService = SupportTicketService(mailRepository)
    private val lifecycleService = SupportTicketLifecycleService(
        ticketRepository = ticketRepository,
        mailRepository = mailRepository,
        supportTicketService = supportTicketService,
        readStateService = readStateService
    )

    @BeforeEach
    fun setUp() {
        reset(ticketRepository, mailRepository, readStateService)
        `when`(ticketRepository.save(anySupportTicket())).thenAnswer { invocation -> invocation.arguments[0] }
        `when`(mailRepository.existsByTicketNumber(anyString())).thenReturn(false)
    }

    /**
     * Keeps the real ticket chain when a normal mail reply carries a manipulated ticket code in the subject.
     */
    @Test
    fun `thread reference wins over manipulated subject ticket number`() {
        val originalTicket = supportTicket("TICKET-111111", "customer@example.org")
        val wrongSubjectTicket = supportTicket("TICKET-222222", "customer@example.org")
        val sentReply = sentReplyMail("<mail-system-reply@example.org>", originalTicket)
        val incomingReply = incomingMail(
            subject = "[TICKET-222222] Re: Password reset",
            senderEmail = "customer@example.org",
            inReplyTo = "<mail-system-reply@example.org>"
        )

        `when`(mailRepository.findByExternalMessageId("<mail-system-reply@example.org>")).thenReturn(sentReply)
        `when`(ticketRepository.findByTicketNumber("TICKET-222222")).thenReturn(wrongSubjectTicket)

        val resolvedTicket = lifecycleService.attachIncomingMail(incomingReply)

        assertSame(originalTicket, resolvedTicket)
        assertSame(originalTicket, incomingReply.ticket)
        kotlin.test.assertEquals("TICKET-111111", incomingReply.ticketNumber)
    }

    /**
     * Prevents a different external sender from attaching a new mail to someone else's ticket by subject only.
     */
    @Test
    fun `subject ticket number from different requester is not trusted`() {
        val existingTicket = supportTicket("TICKET-333333", "owner@example.org")
        val manipulatedMail = incomingMail(
            subject = "[TICKET-333333] Please read my message",
            senderEmail = "attacker@example.org"
        )

        `when`(ticketRepository.findByTicketNumber("TICKET-333333")).thenReturn(existingTicket)

        val resolvedTicket = lifecycleService.attachIncomingMail(manipulatedMail)

        assertNotEquals(existingTicket, resolvedTicket)
        assertNotEquals("TICKET-333333", manipulatedMail.ticketNumber)
    }

    /**
     * Allows the subject fallback when the sender matches the requester stored on the ticket.
     */
    @Test
    fun `subject ticket number remains fallback for same requester`() {
        val existingTicket = supportTicket("TICKET-444444", "customer@example.org")
        val followUpMail = incomingMail(
            subject = "[TICKET-444444] Follow-up without mail headers",
            senderEmail = "customer@example.org"
        )

        `when`(ticketRepository.findByTicketNumber("TICKET-444444")).thenReturn(existingTicket)

        val resolvedTicket = lifecycleService.attachIncomingMail(followUpMail)

        assertSame(existingTicket, resolvedTicket)
        assertSame(existingTicket, followUpMail.ticket)
        kotlin.test.assertEquals("TICKET-444444", followUpMail.ticketNumber)
    }

    private fun supportTicket(ticketNumber: String, requesterEmail: String): SupportTicket {
        return SupportTicket().apply {
            id = UUID.randomUUID()
            this.ticketNumber = ticketNumber
            this.requesterEmail = requesterEmail
            subject = "Password reset"
        }
    }

    private fun sentReplyMail(messageId: String, ticket: SupportTicket): Mail {
        return Mail().apply {
            id = UUID.randomUUID()
            status = MailStatus.SENT
            deliveryMode = MailDeliveryMode.EXTERNAL
            externalMessageId = messageId
            this.ticket = ticket
            ticketNumber = ticket.ticketNumber
        }
    }

    private fun incomingMail(
        subject: String,
        senderEmail: String,
        inReplyTo: String? = null
    ): Mail {
        return Mail().apply {
            id = UUID.randomUUID()
            status = MailStatus.RECEIVED
            deliveryMode = MailDeliveryMode.EXTERNAL
            this.subject = subject
            externalSenderEmail = senderEmail
            externalInReplyTo = inReplyTo
        }
    }

    private fun anySupportTicket(): SupportTicket {
        any(SupportTicket::class.java)
        return SupportTicket()
    }
}
