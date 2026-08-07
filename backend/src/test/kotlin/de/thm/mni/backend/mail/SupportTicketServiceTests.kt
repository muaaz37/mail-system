package de.thm.mni.backend.mail

import de.thm.mni.backend.ticket.SupportTicketService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import kotlin.test.assertEquals

class SupportTicketServiceTests {
    private val mailRepository = mock(MailRepository::class.java)
    private val supportTicketService = SupportTicketService(mailRepository)

    @Test
    fun `reply subject uses fallback when original subject is blank`() {
        val subject = supportTicketService.buildReplySubject("", "TICKET-123456")

        assertEquals("[TICKET-123456] Re: Support request", subject)
    }

    @Test
    fun `reply subject uses fallback when original subject only contains reply prefix`() {
        val subject = supportTicketService.prependTicketIfMissing("Re:", "TICKET-123456")

        assertEquals("[TICKET-123456] Re: Support request", subject)
    }

    @Test
    fun `reply subject keeps original subject and prepends ticket number`() {
        val subject = supportTicketService.buildReplySubject("Cannot open portal", "TICKET-123456")

        assertEquals("[TICKET-123456] Re: Cannot open portal", subject)
    }

    @Test
    fun `reply subject does not duplicate an existing ticket number`() {
        val subject = supportTicketService.prependTicketIfMissing(
            "[TICKET-123456] Re: Cannot open portal",
            "TICKET-123456"
        )

        assertEquals("[TICKET-123456] Re: Cannot open portal", subject)
    }
}
