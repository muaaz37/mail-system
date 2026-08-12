package de.thm.mni.backend.mail

import de.thm.mni.backend.error.ResourceCannotBeModifiedException
import de.thm.mni.backend.mail.dto.MailDTO
import de.thm.mni.backend.mail.dto.MailReplyTemplate
import de.thm.mni.backend.mail.dto.MailRequest
import de.thm.mni.backend.mail.dto.toMailCreate
import de.thm.mni.backend.mail.dto.toMailUpdate
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.internal.InternalMailConversationService
import de.thm.mni.backend.mailrecord.MailRecordService
import de.thm.mni.backend.openapi.BearerAuthenticated
import de.thm.mni.backend.openapi.DefaultApiErrors
import de.thm.mni.backend.openapi.BadGatewayApiResponse
import de.thm.mni.backend.openapi.BadRequestApiResponse
import de.thm.mni.backend.openapi.NotFoundApiResponse
import de.thm.mni.backend.openapi.PayloadTooLargeApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Encoding
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.transaction.annotation.Transactional
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
@BearerAuthenticated
@DefaultApiErrors
@RestController
@RequestMapping("/api/mails", produces = [MediaType.APPLICATION_JSON_VALUE])
class MailController(
    private val mailService: MailService,
    private val mailQueryService: MailQueryService,
    private val mailAccessService: MailAccessService,
    private val mailMapper: MailMapper,
    private val mailReplyService: MailReplyService,
    private val internalMailConversationService: InternalMailConversationService,
    private val mailRecordService: MailRecordService
) {
    /**
     * Returns draft mails owned by the authenticated user.
     */
    @GetMapping("/drafts")
    @Operation(
        operationId = "getDraftMails",
        summary = "List draft mails",
        description = "Returns all draft mails owned by the authenticated user."
    )
    @ApiResponse(responseCode = "200", description = "Draft mails returned successfully.")
    @Transactional(readOnly = true)
    fun getCreatedMails(@AuthenticationPrincipal jwt: Jwt): List<MailDTO> {
        val user = mailAccessService.authenticatedUser(jwt)
        val userMails = mailQueryService.getAllCreatedUserMails(user)
        return userMails.map { mail -> mailMapper.toDTO(user, mail) }
    }

    /**
     * Returns sent mails owned by the authenticated user.
     */
    @GetMapping("/sent")
    @Operation(
        operationId = "getSentMails",
        summary = "List sent mails",
        description = "Returns all mails sent by the authenticated user."
    )
    @ApiResponse(responseCode = "200", description = "Sent mails returned successfully.")
    @Transactional(readOnly = true)
    fun getSentMails(@AuthenticationPrincipal jwt: Jwt): List<MailDTO> {
        val user = mailAccessService.authenticatedUser(jwt)
        val userMails = mailQueryService.getAllSentUserMails(user)
        return userMails.map { mail -> mailMapper.toDTO(user, mail) }
    }

    /**
     * Creates a draft mail from multipart request data and attachments.
     */
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        operationId = "createDraftMail",
        summary = "Create a draft mail",
        description = "Creates a draft from the JSON part named `data` and optional file attachments."
    )
    @RequestBody(
        content = [Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            encoding = [Encoding(name = "data", contentType = MediaType.APPLICATION_JSON_VALUE)]
        )]
    )
    @ApiResponse(responseCode = "201", description = "Draft mail created successfully.")
    @BadRequestApiResponse
    @PayloadTooLargeApiResponse
    fun createMail(
        @Valid @RequestPart("data") data: MailRequest,
        @RequestPart("attachments", required = false) attachments: List<MultipartFile> = emptyList(),
        @AuthenticationPrincipal jwt: Jwt
    ): MailDTO {
        val user = mailAccessService.authenticatedUser(jwt)
        val createdMail = mailService.createMail(data.toMailCreate(), user, attachments)
        return mailMapper.toDTO(user, createdMail)
    }

    /**
     * Returns the user's internal inbox and all imported support mails.
     */
    @GetMapping("/incoming")
    @Operation(
        operationId = "getIncomingMails",
        summary = "List incoming mails",
        description = "Returns internal inbox mails and imported support mails visible to the authenticated user."
    )
    @ApiResponse(responseCode = "200", description = "Incoming mails returned successfully.")
    @Transactional(readOnly = true)
    fun getIncomingMailsForUser(@AuthenticationPrincipal jwt: Jwt): List<MailDTO> {
        val user = mailAccessService.authenticatedUser(jwt)
        val userMails = mailQueryService.getIncomingMailsForUser(user.id!!)
        return userMails.map { mail -> mailMapper.toDTO(user, mail) }
    }

    /**
     * Builds prefilled reply data for a visible internal or external mail.
     */
    @GetMapping("/{mailId}/reply-template")
    @Operation(
        operationId = "getMailReplyTemplate",
        summary = "Get a reply template",
        description = "Builds prefilled reply data for a visible internal or external mail."
    )
    @ApiResponse(responseCode = "200", description = "Reply template returned successfully.")
    @NotFoundApiResponse
    @Transactional(readOnly = true)
    fun getReplyTemplate(
        @Parameter(description = "Identifier of the mail that should be answered.") @PathVariable mailId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): MailReplyTemplate {
        val user = mailAccessService.authenticatedUser(jwt)
        val mail = mailAccessService.mailOrNotFound(mailId)
        mailAccessService.ensureMailVisible(mail, user)
        return mailReplyService.getReplyTemplate(mail, user)
    }

    /**
     * Returns the complete visible conversation containing an internal mail.
     */
    @GetMapping("/{mailId}/conversation")
    @Operation(
        operationId = "getInternalMailConversation",
        summary = "Get an internal mail conversation",
        description = "Returns the root internal mail and all replies in chronological order."
    )
    @ApiResponse(responseCode = "200", description = "Internal conversation returned successfully.")
    @NotFoundApiResponse
    @Transactional
    fun getInternalMailConversation(
        @Parameter(description = "Identifier of any mail in the internal conversation.")
        @PathVariable mailId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): List<MailDTO> {
        val user = mailAccessService.authenticatedUser(jwt)
        val mail = mailAccessService.mailOrNotFound(mailId)
        mailAccessService.ensureMailVisible(mail, user)

        val conversation = internalMailConversationService.getConversation(mail)
            .filter { item -> mailAccessService.canViewMail(item, user) }

        conversation.forEach { item ->
            mailRecordService.markMailRead(requireNotNull(item.id), requireNotNull(user.id))
        }

        return conversation.map { item -> mailMapper.toDTO(user, item) }
    }

    /**
     * Returns a single mail when the authenticated user is allowed to view it.
     */
    @GetMapping("/{mailId}")
    @Operation(
        operationId = "getMailById",
        summary = "Get a mail",
        description = "Returns a mail when the authenticated user is allowed to view it."
    )
    @ApiResponse(responseCode = "200", description = "Mail returned successfully.")
    @NotFoundApiResponse
    @Transactional
    fun getMailById(
        @Parameter(description = "Mail identifier returned by a mail-list operation.") @PathVariable mailId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): MailDTO {
        val user = mailAccessService.authenticatedUser(jwt)
        val mail = mailAccessService.mailOrNotFound(mailId)
        mailAccessService.ensureMailVisible(mail, user)
        if (mail.deliveryMode == MailDeliveryMode.INTERNAL) {
            mailRecordService.markMailRead(mailId, requireNotNull(user.id))
        }
        return mailMapper.toDTO(user, mail)
    }

    /**
     * Updates a draft mail owned by the authenticated user.
     */
    @PutMapping("/{mailId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        operationId = "updateDraftMail",
        summary = "Update a draft mail",
        description = "Replaces the editable content and attachments of a draft owned by the authenticated user."
    )
    @RequestBody(
        content = [Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            encoding = [Encoding(name = "data", contentType = MediaType.APPLICATION_JSON_VALUE)]
        )]
    )
    @ApiResponse(responseCode = "200", description = "Draft mail updated successfully.")
    @BadRequestApiResponse
    @NotFoundApiResponse
    @PayloadTooLargeApiResponse
    fun updateMail(
        @Parameter(description = "Draft identifier returned by `GET /api/mails/drafts`.") @PathVariable mailId: UUID,
        @Valid @RequestPart("data") mail: MailRequest,
        @RequestPart("attachments", required = false) attachments: List<MultipartFile> = emptyList(),
        @AuthenticationPrincipal jwt: Jwt
    ): MailDTO {
        val user = mailAccessService.authenticatedUser(jwt)
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
    @Operation(
        operationId = "deleteMail",
        summary = "Delete a mail",
        description = "Deletes a draft or stored mail owned by the authenticated user."
    )
    @ApiResponse(responseCode = "204", description = "Mail deleted successfully.")
    @NotFoundApiResponse
    fun deleteMail(
        @Parameter(description = "Mail identifier returned by a mail-list operation.")
        @PathVariable mailId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ) {
        val userId = mailAccessService.authenticatedUser(jwt).id!!
        mailService.deleteMail(mailId, userId)
    }

    /**
     * Sends an existing draft mail owned by the authenticated user.
     */
    @PostMapping("/send/{mailId}")
    @Operation(
        operationId = "sendDraftMail",
        summary = "Send a draft mail",
        description = "Sends an existing draft owned by the authenticated user."
    )
    @ApiResponse(responseCode = "200", description = "Draft sent successfully.")
    @NotFoundApiResponse
    @BadGatewayApiResponse
    fun sendMail(
        @Parameter(description = "Draft identifier returned by `GET /api/mails/drafts`.")
        @PathVariable mailId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ) {
        val userId = mailAccessService.authenticatedUser(jwt).id!!
        mailService.sendExistingDraft(mailId, userId)
    }

    /**
     * Creates a mail and sends it in the same request.
     */
    @PostMapping("/send", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        operationId = "createAndSendMail",
        summary = "Create and send a mail",
        description = "Creates a mail from multipart data and sends it immediately."
    )
    @RequestBody(
        content = [Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            encoding = [Encoding(name = "data", contentType = MediaType.APPLICATION_JSON_VALUE)]
        )]
    )
    @ApiResponse(responseCode = "200", description = "Mail created and sent successfully.")
    @BadRequestApiResponse
    @PayloadTooLargeApiResponse
    @BadGatewayApiResponse
    fun createAndSendMail(
        @Valid @RequestPart("data") data: MailRequest,
        @RequestPart("attachments", required = false) attachments: List<MultipartFile> = emptyList(),
        @AuthenticationPrincipal jwt: Jwt
    ): MailDTO {
        val user = mailAccessService.authenticatedUser(jwt)
        val createdMail = mailService.createAndSendMail(data.toMailCreate(), user, attachments)
        return mailMapper.toDTO(user, createdMail)
    }
}
