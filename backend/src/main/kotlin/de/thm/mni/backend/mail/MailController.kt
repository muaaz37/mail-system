package de.thm.mni.backend.mail

import de.thm.mni.backend.error.ResourceCannotBeModifiedException
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail.dto.MailRequest
import de.thm.mni.backend.mail.dto.MailDTO
import de.thm.mni.backend.mail.dto.toMailCreate
import de.thm.mni.backend.mail.dto.toMailUpdate
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail_record.MailRecordService
import de.thm.mni.backend.user.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID


@RestController
@RequestMapping("/api/mails")
class MailController(private val mailService: MailService,
                     private val userService: UserService,
                     private val mailRecordService: MailRecordService,
                     private val mailMapper: MailMapper
    ) {

    @GetMapping("/drafts")
    fun getCreatedMails(@AuthenticationPrincipal user: UserDetails): List<MailDTO> {
        val userId = UUID.fromString(user.username)
        val user = userService.getUserById(userId) ?: throw ResourceNotFoundException("User not found")
        val userMails =  mailService.getAllCreatedUserMails(user)
        return userMails.map { mail -> mailMapper.toDTO(user, mail) }
    }

    @GetMapping("/sent")
    fun getSentMails(@AuthenticationPrincipal user: UserDetails): List<MailDTO> {
        val userId = UUID.fromString(user.username)
        val user = userService.getUserById(userId) ?: throw ResourceNotFoundException("User not found")
        val userMails =  mailService.getAllSentUserMails(user)
        return userMails.map { mail -> mailMapper.toDTO(user, mail) }

    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun createMail(@Valid @RequestPart("data") data: MailRequest,
                   @RequestPart("attachments") attachments: List<MultipartFile>,
                   @AuthenticationPrincipal user: UserDetails): MailDTO
    {
        val userId = UUID.fromString(user.username)
        val user = userService.getUserById(userId) ?: throw ResourceNotFoundException("User not found")

        val createdMail = mailService.createMail(data.toMailCreate(), user, attachments)
        return mailMapper.toDTO(user, createdMail)
    }

    @GetMapping("/incoming")
    fun getIncomingMailsForUser(@AuthenticationPrincipal user: UserDetails): List<MailDTO> {
        val userId = UUID.fromString(user.username)
        val user = userService.getUserById(userId) ?: throw ResourceNotFoundException("User not found")
        val userMails = mailRecordService.getAllIncomingMailsForUser(userId)
        return userMails.map { mail -> mailMapper.toDTO(user, mail) }
    }

    @GetMapping("/{mailId}")
    fun getMailById(@PathVariable mailId: UUID, @AuthenticationPrincipal user: UserDetails): MailDTO {
        val user = userService.getUserById(UUID.fromString(user.username)) ?: throw ResourceNotFoundException("User not found")
        val mail = mailService.getMailById(mailId) ?: throw ResourceNotFoundException("Mail not found")
        val records = mailRecordService.getMailRecordByMailId(mail.id!!)

        // Check if the user is either the sender or a recipient of the mail
        if (records.none { it.user!!.id == user.id } && mail.sender!!.id != user.id) {
            throw ResourceNotFoundException("Mail not found")
        }
        return mailMapper.toDTO(user, mail)
    }

    @PutMapping("/{mailId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateMail(@PathVariable mailId: UUID,
                   @Valid @RequestPart("data") mail: MailRequest,
                   @RequestPart("attachments") attachments: List<MultipartFile>,
                   @AuthenticationPrincipal user: UserDetails): MailDTO
    {
        val userId = UUID.fromString(user.username)
        val user = userService.getUserById(userId) ?: throw ResourceNotFoundException("User not found")
        val existingMail = mailService.getMailById(mailId) ?: throw ResourceNotFoundException("Mail not found")

        if (existingMail.sender!!.id != userId) {
            throw ResourceNotFoundException("Mail not found")
        }

        if (existingMail.status == MailStatus.SENT) {
            throw ResourceCannotBeModifiedException("Cannot update a sent mail")
        }

        val updatedMail = mailService.updateMail(mailId, mail.toMailUpdate(), attachments)
        return mailMapper.toDTO(user, updatedMail)
    }

    @DeleteMapping("/{mailId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMail(@PathVariable mailId: UUID, @AuthenticationPrincipal user: UserDetails) {
        val userId = UUID.fromString(user.username)
        val existingMail = mailService.getMailById(mailId) ?: throw ResourceNotFoundException("Mail not found")
        if (existingMail.sender!!.id != userId) {
            throw ResourceNotFoundException("Mail not found")
        }
        mailService.deleteMail(existingMail)
    }

    @PostMapping("/send/{mailId}")
    fun sendMail(@PathVariable mailId: UUID, @AuthenticationPrincipal user: UserDetails) {
        val userId = UUID.fromString(user.username)
        val existingMail = mailService.getMailById(mailId) ?: throw ResourceNotFoundException("Mail not found")
        if (existingMail.sender!!.id != userId) {
            throw ResourceNotFoundException("Mail not found")
        }
        mailService.sendMail(existingMail)
    }

    @PostMapping("/send")
    fun createAndSendMail(@Valid @RequestPart("data") data: MailRequest,
                          @RequestPart("attachments") attachments: List<MultipartFile>,
                          @AuthenticationPrincipal user: UserDetails): MailDTO {
        val userId = UUID.fromString(user.username)
        val user = userService.getUserById(userId) ?: throw ResourceNotFoundException("User not found")

        val createdMail = mailService.createAndSendMail(data.toMailCreate(), user, attachments)
        return mailMapper.toDTO(user, createdMail)
    }

}