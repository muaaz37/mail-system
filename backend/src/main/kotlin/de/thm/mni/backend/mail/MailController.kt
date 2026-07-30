package de.thm.mni.backend.mail

import de.thm.mni.backend.error.ResourceCannotBeModifiedException
import de.thm.mni.backend.mail.dto.MailDTO
import de.thm.mni.backend.mail.dto.MailReplyTemplate
import de.thm.mni.backend.mail.dto.MailRequest
import de.thm.mni.backend.mail.dto.toMailCreate
import de.thm.mni.backend.mail.dto.toMailUpdate
import de.thm.mni.backend.mail.enums.MailStatus
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

/**
 * Provides the HTTP API for drafts, sent mails, inbox mails and support replies.
 */
@Tag(name = "Mail", description = "Manage drafts, sent mails, inbox mails and support replies.")
@RestController
@RequestMapping("/api/mails")
class MailController(
    private val mailService: MailService,
    private val mailAccessService: MailAccessService,
    private val mailMapper: MailMapper,
    private val supportReplyService: SupportReplyService
) {
    /**
     * Returns draft mails owned by the authenticated user.
     */
    @GetMapping("/drafts")
    fun getCreatedMails(@AuthenticationPrincipal userDetails: UserDetails): List<MailDTO> {
        val user = mailAccessService.authenticatedUser(userDetails)
        val userMails = mailService.getAllCreatedUserMails(user)
        return userMails.map { mail -> mailMapper.toDTO(user, mail) }
    }

    /**
     * Returns sent mails owned by the authenticated user.
     */
    @GetMapping("/sent")
    fun getSentMails(@AuthenticationPrincipal userDetails: UserDetails): List<MailDTO> {
        val user = mailAccessService.authenticatedUser(userDetails)
        val userMails = mailService.getAllSentUserMails(user)
        return userMails.map { mail -> mailMapper.toDTO(user, mail) }
    }

    /**
     * Creates a draft mail from multipart request data and attachments.
     */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun createMail(
        @Valid @RequestPart("data") data: MailRequest,
        @RequestPart("attachments") attachments: List<MultipartFile>,
        @AuthenticationPrincipal userDetails: UserDetails
    ): MailDTO {
        val user = mailAccessService.authenticatedUser(userDetails)
        val createdMail = mailService.createMail(data.toMailCreate(), user, attachments)
        return mailMapper.toDTO(user, createdMail)
    }

    /**
     * Returns the user's internal inbox and all imported support mails.
     */
    @GetMapping("/incoming")
    fun getIncomingMailsForUser(@AuthenticationPrincipal userDetails: UserDetails): List<MailDTO> {
        val user = mailAccessService.authenticatedUser(userDetails)
        val userMails = mailService.getIncomingMailsForUser(user.id!!)
        return userMails.map { mail -> mailMapper.toDTO(user, mail) }
    }

    /**
     * Builds a prefilled reply template for an imported support mail.
     */
    @GetMapping("/{mailId}/reply-template")
    fun getReplyTemplate(
        @PathVariable mailId: UUID,
        @AuthenticationPrincipal userDetails: UserDetails
    ): MailReplyTemplate {
        val user = mailAccessService.authenticatedUser(userDetails)
        val mail = mailAccessService.mailOrNotFound(mailId)
        mailAccessService.ensureMailVisible(mail, user)
        return supportReplyService.getReplyTemplate(mail)
    }

    /**
     * Returns a single mail when the authenticated user is allowed to view it.
     */
    @GetMapping("/{mailId}")
    fun getMailById(
        @PathVariable mailId: UUID,
        @AuthenticationPrincipal userDetails: UserDetails
    ): MailDTO {
        val user = mailAccessService.authenticatedUser(userDetails)
        val mail = mailAccessService.mailOrNotFound(mailId)
        mailAccessService.ensureMailVisible(mail, user)
        return mailMapper.toDTO(user, mail)
    }

    /**
     * Updates a draft mail owned by the authenticated user.
     */
    @PutMapping("/{mailId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateMail(
        @PathVariable mailId: UUID,
        @Valid @RequestPart("data") mail: MailRequest,
        @RequestPart("attachments") attachments: List<MultipartFile>,
        @AuthenticationPrincipal userDetails: UserDetails
    ): MailDTO {
        val user = mailAccessService.authenticatedUser(userDetails)
        val existingMail = mailAccessService.mailOrNotFound(mailId)
        mailAccessService.ensureOwnedBy(existingMail, user.id!!)

        if (existingMail.status == MailStatus.SENT) {
            throw ResourceCannotBeModifiedException("Cannot update a sent mail")
        }

        val updatedMail = mailService.updateMail(mailId, mail.toMailUpdate(), attachments)
        return mailMapper.toDTO(user, updatedMail)
    }

    /**
     * Deletes a draft or stored mail owned by the authenticated user.
     */
    @DeleteMapping("/{mailId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMail(@PathVariable mailId: UUID, @AuthenticationPrincipal userDetails: UserDetails) {
        val userId = UUID.fromString(userDetails.username)
        val existingMail = mailAccessService.mailOrNotFound(mailId)
        mailAccessService.ensureOwnedBy(existingMail, userId)
        mailService.deleteMail(existingMail)
    }

    /**
     * Sends an existing draft mail owned by the authenticated user.
     */
    @PostMapping("/send/{mailId}")
    fun sendMail(@PathVariable mailId: UUID, @AuthenticationPrincipal userDetails: UserDetails) {
        val userId = UUID.fromString(userDetails.username)
        val existingMail = mailAccessService.mailOrNotFound(mailId)
        mailAccessService.ensureOwnedBy(existingMail, userId)
        mailService.sendMail(existingMail)
    }

    /**
     * Creates a mail and sends it in the same request.
     */
    @PostMapping("/send")
    fun createAndSendMail(
        @Valid @RequestPart("data") data: MailRequest,
        @RequestPart("attachments") attachments: List<MultipartFile>,
        @AuthenticationPrincipal userDetails: UserDetails
    ): MailDTO {
        val user = mailAccessService.authenticatedUser(userDetails)
        val createdMail = mailService.createAndSendMail(data.toMailCreate(), user, attachments)
        return mailMapper.toDTO(user, createdMail)
    }
}
