package de.thm.mni.backend.mail

import de.thm.mni.backend.error.MailSendFailedException
import de.thm.mni.backend.error.ResourceCannotBeModifiedException
import de.thm.mni.backend.error.ResourceNotFoundException
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
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import de.thm.mni.backend.user.User

class MailServiceTests {
    private val mailRepository = mock(MailRepository::class.java)
    private val mailSender = mock(MailSender::class.java)
    private val mailRecordService = mock(MailRecordService::class.java)
    private val mailAttachmentHandler = mock(MailAttachmentHandler::class.java)
    private val ticketLifecycleService = mock(SupportTicketLifecycleService::class.java)
    private val mailService = MailService(
        mailRepository,
        mailSender,
        mailRecordService,
        mock(MailRecipientValidator::class.java),
        mailAttachmentHandler,
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

    @Test
    fun `existing draft is loaded with attachments before sending`() {
        val mailId = UUID.randomUUID()
        val senderId = UUID.randomUUID()
        val mail = draft(MailDeliveryMode.EXTERNAL).apply {
            id = mailId
            sender = User().apply { id = senderId }
            externalTo = "customer@example.org"
        }
        `when`(mailRepository.findByIdWithAttachments(mailId)).thenReturn(mail)
        `when`(mailSender.send(mail)).thenReturn(true)
        `when`(mailRepository.save(mail)).thenReturn(mail)

        val sentMail = mailService.sendExistingDraft(mailId, senderId)

        assertEquals(MailStatus.SENT, sentMail.status)
        verify(mailRepository).findByIdWithAttachments(mailId)
        verify(mailSender).send(mail)
    }

    @Test
    fun `existing draft owned by another user is hidden`() {
        val mailId = UUID.randomUUID()
        val senderId = UUID.randomUUID()
        val mail = draft(MailDeliveryMode.EXTERNAL).apply {
            id = mailId
            sender = User().apply { id = UUID.randomUUID() }
        }
        `when`(mailRepository.findByIdWithAttachments(mailId)).thenReturn(mail)

        assertFailsWith<ResourceNotFoundException> {
            mailService.sendExistingDraft(mailId, senderId)
        }

        verify(mailSender, never()).send(mail)
        verify(mailRepository, never()).save(mail)
    }

    @Test
    fun `existing mail is loaded with attachments before deleting`() {
        val mailId = UUID.randomUUID()
        val senderId = UUID.randomUUID()
        val mail = draft(MailDeliveryMode.EXTERNAL).apply {
            id = mailId
            sender = User().apply { id = senderId }
        }
        `when`(mailRepository.findByIdWithAttachments(mailId)).thenReturn(mail)
        `when`(mailRecordService.getMailRecordByMailId(mailId)).thenReturn(emptyList())

        mailService.deleteMail(mailId, senderId)

        verify(mailRepository).findByIdWithAttachments(mailId)
        verify(mailAttachmentHandler).deleteAttachments(mail)
        verify(mailRepository).delete(mail)
    }

    @Test
    fun `existing mail owned by another user is hidden before deleting`() {
        val mailId = UUID.randomUUID()
        val senderId = UUID.randomUUID()
        val mail = draft(MailDeliveryMode.EXTERNAL).apply {
            id = mailId
            sender = User().apply { id = UUID.randomUUID() }
        }
        `when`(mailRepository.findByIdWithAttachments(mailId)).thenReturn(mail)

        assertFailsWith<ResourceNotFoundException> {
            mailService.deleteMail(mailId, senderId)
        }

        verify(mailRepository, never()).delete(mail)
    }

    private fun draft(deliveryMode: MailDeliveryMode): Mail = Mail().apply {
        this.deliveryMode = deliveryMode
    }
}
