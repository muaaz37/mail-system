package de.thm.mni.backend.mail

import de.thm.mni.backend.mail.dto.ExternalMailReplyTemplate
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.user.User
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.assertSame

class MailReplyServiceTests {
    private val mailRepository = mock(MailRepository::class.java)
    private val internalHandler = mock(MailReplyHandler::class.java)
    private val externalHandler = mock(MailReplyHandler::class.java)

    @Test
    fun `reply template is delegated to handler for delivery mode`() {
        `when`(internalHandler.deliveryMode).thenReturn(MailDeliveryMode.INTERNAL)
        `when`(externalHandler.deliveryMode).thenReturn(MailDeliveryMode.EXTERNAL)

        val replyService = MailReplyService(
            mailRepository = mailRepository,
            replyHandlers = listOf(internalHandler, externalHandler)
        )
        val originalMail = Mail().apply {
            deliveryMode = MailDeliveryMode.EXTERNAL
        }
        val currentUser = User()
        val expectedTemplate = ExternalMailReplyTemplate(
            replyToMailId = UUID.randomUUID(),
            subject = "Re: Support request",
            ticketNumber = "TICKET-123456",
            recipients = listOf("customer@example.org")
        )
        `when`(externalHandler.getReplyTemplate(originalMail, currentUser))
            .thenReturn(expectedTemplate)

        val actualTemplate = replyService.getReplyTemplate(originalMail, currentUser)

        assertSame(expectedTemplate, actualTemplate)
        verify(externalHandler).getReplyTemplate(originalMail, currentUser)
    }
}
