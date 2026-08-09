package de.thm.mni.backend.mail

import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.external.SupportReplyService
import de.thm.mni.backend.ticket.SupportTicketLifecycleService
import de.thm.mni.backend.ticket.SupportTicketService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import kotlin.test.assertEquals

class SupportReplyServiceTests {
    private val mailRepository = mock(MailRepository::class.java)
    private val supportTicketService = SupportTicketService(mailRepository)
    private val supportTicketLifecycleService = mock(SupportTicketLifecycleService::class.java)
    private val supportReplyService = SupportReplyService(
        supportTicketService = supportTicketService,
        supportTicketLifecycleService = supportTicketLifecycleService
    )

    @Test
    fun `ticket subject is restored for stored support reply drafts`() {
        val draft = Mail().apply {
            status = MailStatus.DRAFT
            deliveryMode = MailDeliveryMode.EXTERNAL
            ticketNumber = "TICKET-123456"
            subject = "Re: Cannot open portal"
        }

        supportReplyService.enforceTicketSubject(draft)

        assertEquals("[TICKET-123456] Re: Cannot open portal", draft.subject)
    }
}
