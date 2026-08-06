package de.thm.mni.backend.mail

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
}
