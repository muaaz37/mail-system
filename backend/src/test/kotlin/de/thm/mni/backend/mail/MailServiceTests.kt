package de.thm.mni.backend.mail

import de.thm.mni.backend.error.MailSendFailedException
import de.thm.mni.backend.error.ResourceCannotBeModifiedException
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.external.SupportReplyService
import de.thm.mni.backend.mail.validation.MailRecipientValidator
import de.thm.mni.backend.mailrecord.MailRecordService
import de.thm.mni.backend.ticket.SupportTicketLifecycleService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MailServiceTests {
    private val mailRepository = mock(MailRepository::class.java)
    private val mailSender = mock(MailSender::class.java)
    private val ticketLifecycleService = mock(SupportTicketLifecycleService::class.java)
    private val mailService = MailService(
        mailRepository,
        mailSender,
        mock(MailRecordService::class.java),
        mock(MailRecipientValidator::class.java),
        mock(MailAttachmentHandler::class.java),
        mock(SupportReplyService::class.java),
        ticketLifecycleService,
        mock(MailReplyService::class.java)
    )

    @Test
    fun `internal draft is sent without external mail sender`() {
        val mail = draft(MailDeliveryMode.INTERNAL)
        `when`(mailRepository.save(mail)).thenReturn(mail)

        val sentMail = mailService.sendMail(mail)

        assertEquals(MailStatus.SENT, sentMail.status)
        verify(mailSender, never()).send(mail)
    }

    @Test
    fun `failed external delivery keeps draft for retry`() {
        val mail = draft(MailDeliveryMode.EXTERNAL)
        `when`(mailSender.send(mail)).thenReturn(false)

        assertFailsWith<MailSendFailedException> { mailService.sendMail(mail) }

        assertEquals(MailStatus.DRAFT, mail.status)
        verify(mailRepository, never()).save(mail)
    }

    @Test
    fun `sent mail cannot be sent again`() {
        val mail = draft(MailDeliveryMode.INTERNAL).apply { markAsSent() }

        assertFailsWith<ResourceCannotBeModifiedException> { mailService.sendMail(mail) }
        verify(mailRepository, never()).save(mail)
    }

    private fun draft(deliveryMode: MailDeliveryMode): Mail = Mail().apply {
        this.deliveryMode = deliveryMode
    }
}
