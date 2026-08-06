import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, Input, OnChanges, OnInit, signal } from '@angular/core';
import {
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { FileRemoveEvent, FileSelectEvent, FileUploadModule } from 'primeng/fileupload';
import { ImageModule } from 'primeng/image';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { TextareaModule } from 'primeng/textarea';
import { Toast } from 'primeng/toast';
import { switchMap } from 'rxjs';
import { AuthService } from '../../../services/auth/auth-service';
import { MailsService } from '../../../services/mails/mails-service';
import { Attachment } from '../../../types/attachment';
import {
  CreateMail,
  Mail,
  MailDeliveryMode,
  MailReplyTemplate,
  MailStatus,
} from '../../../types/mails';
import { User } from '../../../types/user';
import { readApiErrorMessage } from '../../../utils/api-error-message';

@Component({
  selector: 'app-mail-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    ChipModule,
    ButtonModule,
    MultiSelectModule,
    TextareaModule,
    FileUploadModule,
    Toast,
    InputTextModule,
    ImageModule,
  ],
  templateUrl: './mail-form.html',
})
export class MailForm implements OnInit, OnChanges {
  @Input() mailData: Mail | null = null;
  @Input() replyTemplate: MailReplyTemplate | null = null;
  @Input() title = 'Create Mail';

  private mailsService = inject(MailsService);
  private messageService = inject(MessageService);
  private authService = inject(AuthService);
  private router = inject(Router);

  protected mailForm = new FormGroup({
    subject: new FormControl('', [Validators.required, Validators.maxLength(500)]),
    content: new FormControl('', [Validators.required, Validators.maxLength(10000)]),
    deliveryMode: new FormControl<MailDeliveryMode>(MailDeliveryMode.INTERNAL, {
      nonNullable: true,
    }),
    externalTo: new FormControl(''),
    externalCc: new FormControl(''),
    externalBcc: new FormControl(''),
    externalReplyTo: new FormControl(''),
  });

  protected availableUsers = signal<User[]>([]);
  protected selectedToUsers = signal<string[]>([]);
  protected selectedCcUsers = signal<string[]>([]);
  protected selectedBccUsers = signal<string[]>([]);
  protected selectedReplyToUsers = signal<string[]>([]);
  protected uploadedFiles = signal<File[]>([]);
  protected isLoading = signal(false);
  protected attachments = signal<Attachment[]>([]);
  protected readonly acceptedAttachmentTypes = 'image/*,application/pdf,.pdf';
  protected readonly maxAttachmentSizeBytes = 10 * 1024 * 1024;
  protected readonly maxAttachmentSizeLabel = '10 MB';
  protected readonly MailDeliveryMode = MailDeliveryMode;

  /**
   * Returns whether the form is currently composing a reply to an imported support mail.
   *
   * @returns True when backend-provided reply metadata is active.
   */
  protected isSupportReply(): boolean {
    return this.replyTemplate !== null || this.hasStoredTicketContext();
  }

  /**
   * Returns the immutable ticket number shown next to the editable reply subject.
   *
   * @returns Ticket number for support replies or null for regular mails.
   */
  protected replyTicketNumber(): string | null {
    return this.replyTemplate?.ticketNumber ?? this.mailData?.ticketNumber ?? null;
  }

  /**
   * Loads selectable internal recipients and initializes the form state.
   */
  ngOnInit(): void {
    this.loadUsers();
    this.fillForm();
  }

  /**
   * Rebuilds the form when a draft or reply template arrives asynchronously.
   */
  ngOnChanges(): void {
    this.fillForm();
  }

  /**
   * Returns the currently selected delivery mode.
   *
   * @returns Internal or external delivery mode selected in the form.
   */
  protected deliveryMode(): MailDeliveryMode {
    return this.mailForm.controls.deliveryMode.value;
  }

  /**
   * Determines whether the component edits an existing draft.
   *
   * @returns True when the current mail data is an editable draft.
   */
  protected isDraftEdit(): boolean {
    return this.mailData?.status === MailStatus.DRAFT;
  }

  /**
   * Switches between internal and external recipient fields while keeping reply rules valid.
   *
   * @param mode Delivery mode selected by the user.
   */
  protected setDeliveryMode(mode: MailDeliveryMode): void {
    if (this.isSupportReply() && mode !== MailDeliveryMode.EXTERNAL) {
      this.showWarning('Support replies must be sent as external mails.');
      return;
    }

    this.mailForm.controls.deliveryMode.setValue(mode);
    if (mode === MailDeliveryMode.INTERNAL) {
      this.clearExternalRecipients();
    } else {
      this.clearInternalRecipients();
    }
  }

  /**
   * Loads local user profiles for internal recipient selection and excludes the current user.
   */
  private loadUsers(): void {
    const currentUserId = this.authService.getCurrentUser()?.id;
    this.mailsService.getAllUsers().subscribe({
      next: (users) => {
        this.availableUsers.set(users.filter((user) => user.id !== currentUserId));
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to load users',
        });
      },
    });
  }

  /**
   * Selects the correct initialization path for draft editing or support replies.
   */
  private fillForm(): void {
    if (this.mailData) {
      this.fillFromExistingMail();
      return;
    }

    if (this.replyTemplate) {
      this.fillFromReplyTemplate();
    }
  }

  /**
   * Copies an existing draft into the reactive form and local selection signals.
   */
  private fillFromExistingMail(): void {
    if (!this.mailData) {
      return;
    }

    this.mailForm.patchValue({
      subject: this.editableReplySubject(this.mailData.subject),
      content: this.mailData.content,
      deliveryMode: this.mailData.deliveryMode,
      externalTo: this.mailData.externalTo.join(', '),
      externalCc: this.mailData.externalCc.join(', '),
      externalBcc: this.mailData.externalBcc.join(', '),
      externalReplyTo: this.mailData.externalReplyTo.join(', '),
    });

    this.selectedToUsers.set(this.mailData.to.map((user) => user.id));
    this.selectedCcUsers.set(this.mailData.cc.map((user) => user.id));
    this.selectedBccUsers.set(this.mailData.bcc.map((user) => user.id));
    this.selectedReplyToUsers.set(this.mailData.replyTo.map((user) => user.id));
    this.attachments.set(this.mailData.attachments);
  }

  /**
   * Prefills an external reply with backend-generated recipient and subject data.
   */
  private fillFromReplyTemplate(): void {
    if (!this.replyTemplate) {
      return;
    }

    this.mailForm.patchValue({
      subject: this.editableReplySubject(this.replyTemplate.subject),
      content: '',
      deliveryMode: MailDeliveryMode.EXTERNAL,
      externalTo: this.replyTemplate.externalTo.join(', '),
      externalCc: this.replyTemplate.externalCc.join(', '),
      externalBcc: this.replyTemplate.externalBcc.join(', '),
      externalReplyTo: this.replyTemplate.externalReplyTo.join(', '),
    });
    this.clearInternalRecipients();
  }

  /**
   * Adds newly selected browser files to the current upload queue.
   *
   * @param event PrimeNG file selection event containing selected files.
   */
  onFileSelect(event: FileSelectEvent): void {
    this.uploadedFiles.set([...this.uploadedFiles(), ...event.files]);
  }

  /**
   * Removes a newly selected browser file from the upload queue.
   *
   * @param event PrimeNG file removal event containing the removed file.
   */
  onFileRemove(event: FileRemoveEvent): void {
    this.uploadedFiles.set(this.uploadedFiles().filter((file) => file !== event.file));
  }

  /**
   * Removes an already persisted attachment from the draft's retained attachment list.
   *
   * @param attachment Existing attachment selected for removal.
   */
  onExistingFileRemove(attachment: Attachment): void {
    this.attachments.set(this.attachments().filter((att) => att.url !== attachment.url));
  }

  /**
   * Checks whether an attachment can be displayed as an image preview.
   *
   * @param attachment Attachment metadata returned by the backend.
   * @returns True when the attachment has an image MIME type.
   */
  protected isImageAttachment(attachment: Attachment): boolean {
    return attachment.mimeType?.startsWith('image/') ?? false;
  }

  /**
   * Checks whether an attachment should be handled as a PDF preview.
   *
   * @param attachment Attachment metadata returned by the backend.
   * @returns True when the attachment has a PDF MIME type or filename.
   */
  protected isPdfAttachment(attachment: Attachment): boolean {
    return attachment.mimeType === 'application/pdf' || attachment.fileName.toLowerCase().endsWith('.pdf');
  }

  /**
   * Checks whether at least one team profile is selected as an internal recipient.
   *
   * @returns True when To, Cc or Bcc contains an internal user.
   */
  private internalRecipientsNotEmpty(): boolean {
    return (
      this.selectedToUsers().length > 0 ||
      this.selectedCcUsers().length > 0 ||
      this.selectedBccUsers().length > 0
    );
  }

  /**
   * Checks whether at least one external recipient address is entered.
   *
   * @returns True when To, Cc or Bcc contains an external address.
   */
  private externalRecipientsNotEmpty(): boolean {
    return (
      this.parseEmailList(this.mailForm.controls.externalTo.value).length > 0 ||
      this.parseEmailList(this.mailForm.controls.externalCc.value).length > 0 ||
      this.parseEmailList(this.mailForm.controls.externalBcc.value).length > 0
    );
  }

  /**
   * Validates required fields and recipient rules before draft or send requests are made.
   *
   * @returns True when the current form state can be submitted.
   */
  private validateForm(): boolean {
    if (this.mailForm.invalid) {
      this.showWarning('Please fill in all required fields');
      return false;
    }

    if (this.deliveryMode() === MailDeliveryMode.INTERNAL && !this.internalRecipientsNotEmpty()) {
      this.showWarning('Please select at least one team user');
      return false;
    }

    if (this.deliveryMode() === MailDeliveryMode.EXTERNAL && !this.externalRecipientsNotEmpty()) {
      this.showWarning('Please enter at least one external email address');
      return false;
    }

    if (this.deliveryMode() === MailDeliveryMode.EXTERNAL && !this.externalEmailsValid()) {
      this.showWarning('Please enter valid external email addresses');
      return false;
    }

    return true;
  }

  /**
   * Converts the UI form state into the backend mail command payload.
   *
   * @returns Mail payload for create, update and send operations.
   */
  private buildMailData(): CreateMail {
    return {
      subject: this.completeSubject(),
      content: this.mailForm.controls.content.value || '',
      deliveryMode: this.deliveryMode(),
      toIds: this.deliveryMode() === MailDeliveryMode.INTERNAL ? this.selectedToUsers() : [],
      ccIds: this.deliveryMode() === MailDeliveryMode.INTERNAL ? this.selectedCcUsers() : [],
      bccIds: this.deliveryMode() === MailDeliveryMode.INTERNAL ? this.selectedBccUsers() : [],
      replyToIds:
        this.deliveryMode() === MailDeliveryMode.INTERNAL ? this.selectedReplyToUsers() : [],
      externalTo:
        this.deliveryMode() === MailDeliveryMode.EXTERNAL
          ? this.parseEmailList(this.mailForm.controls.externalTo.value)
          : [],
      externalCc:
        this.deliveryMode() === MailDeliveryMode.EXTERNAL
          ? this.parseEmailList(this.mailForm.controls.externalCc.value)
          : [],
      externalBcc:
        this.deliveryMode() === MailDeliveryMode.EXTERNAL
          ? this.parseEmailList(this.mailForm.controls.externalBcc.value)
          : [],
      externalReplyTo:
        this.deliveryMode() === MailDeliveryMode.EXTERNAL
          ? this.parseEmailList(this.mailForm.controls.externalReplyTo.value)
          : [],
      replyToMailId: this.replyTemplate?.replyToMailId ?? null,
    };
  }

  /**
   * Builds the full attachment file list sent to the backend replacement endpoint.
   *
   * @returns New uploads plus retained existing attachments represented as File objects.
   */
  private buildAttachmentData(): File[] {
    const newAttachments = this.uploadedFiles().map((file) => file);
    const existingAttachments = this.attachments()
      .filter((attachment) => attachment.blob)
      .map((attachment) => this.blobToFile(attachment.blob!, attachment.fileName));

    // The backend replaces the complete attachment set, so retained files must be sent again.
    return [...newAttachments, ...existingAttachments];
  }

  /**
   * Normalizes the editable part to exactly one reply prefix and no ticket number.
   *
   * @param subject Subject value from a reply template or form input.
   * @returns Editable subject without the immutable ticket prefix.
   */
  private editableReplySubject(subject: string): string {
    if (!this.isSupportReply()) {
      return subject;
    }

    const withoutTickets = subject.replace(/\[?\s*TICKET-\d+\s*]?/gi, ' ').trim();
    const baseSubject = withoutTickets.replace(/^(?:\s*Re\s*:\s*)+/i, '').trim();
    return `Re: ${baseSubject}`.trim();
  }

  /**
   * Restores the immutable ticket prefix before the reply is sent to the backend.
   *
   * @returns Full subject including the support ticket number for replies.
   */
  private completeSubject(): string {
    const subject = this.mailForm.controls.subject.value?.trim() ?? '';
    const ticketNumber = this.replyTicketNumber();
    return ticketNumber ? `[${ticketNumber}] ${this.editableReplySubject(subject)}`.trim() : subject;
  }

  /**
   * Detects persisted support reply drafts where no fresh reply template is available.
   *
   * @returns True when an existing external draft is already linked to a support ticket.
   */
  private hasStoredTicketContext(): boolean {
    return this.mailData?.deliveryMode === MailDeliveryMode.EXTERNAL && !!this.mailData.ticketNumber;
  }

  /**
   * Parses comma, semicolon, whitespace and `Name <mail@example.org>` recipient input.
   *
   * @param value Raw input from an external recipient field.
   * @returns Unique email addresses extracted from the input.
   */
  private parseEmailList(value: string | null): string[] {
    const input = value || '';
    const namedAddressEmails = [...input.matchAll(/<([^<>\s]+@[^<>\s]+)>/g)].map(
      (match) => match[1],
    );
    const plainAddressInput = input.replace(/[^,;]*<[^<>]+>/g, ' ');
    const plainAddressEmails = plainAddressInput
      .split(/[;,\s]+/)
      .map((email) => email.trim())
      .filter(Boolean);

    return [...namedAddressEmails, ...plainAddressEmails].filter(
      (email, index, emails) => emails.indexOf(email) === index,
    );
  }

  /**
   * Validates all entered external email addresses with a conservative browser-side check.
   *
   * @returns True when every parsed external address has a basic email shape.
   */
  private externalEmailsValid(): boolean {
    const emails = [
      ...this.parseEmailList(this.mailForm.controls.externalTo.value),
      ...this.parseEmailList(this.mailForm.controls.externalCc.value),
      ...this.parseEmailList(this.mailForm.controls.externalBcc.value),
      ...this.parseEmailList(this.mailForm.controls.externalReplyTo.value),
    ];
    return emails.every((email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email));
  }

  /**
   * Clears selected internal recipients when external delivery is selected.
   */
  private clearInternalRecipients(): void {
    this.selectedToUsers.set([]);
    this.selectedCcUsers.set([]);
    this.selectedBccUsers.set([]);
    this.selectedReplyToUsers.set([]);
  }

  /**
   * Clears external recipient fields when internal delivery is selected.
   */
  private clearExternalRecipients(): void {
    this.mailForm.patchValue({
      externalTo: '',
      externalCc: '',
      externalBcc: '',
      externalReplyTo: '',
    });
  }

  /**
   * Shows a warning toast for recoverable user input problems.
   *
   * @param detail User-facing warning text.
   */
  private showWarning(detail: string): void {
    this.messageService.add({
      severity: 'warn',
      summary: 'Warning',
      detail,
    });
  }

  /**
   * Wraps an already loaded attachment blob as a File for multipart replacement uploads.
   *
   * @param blob Existing attachment binary loaded from the backend.
   * @param filename Original attachment filename.
   * @returns File object that can be appended to multipart form data.
   */
  private blobToFile(blob: Blob, filename: string): File {
    return new File([blob], filename, { type: blob.type });
  }

  /**
   * Shows a success toast, resets local form state and navigates to the target mailbox view.
   *
   * @param message User-facing success message.
   * @param navigateTo Route that should be opened after the operation succeeds.
   */
  private handleMailSuccess(message: string, navigateTo: string): void {
    this.messageService.add({
      severity: 'success',
      summary: 'Success',
      detail: message,
    });
    this.resetForm();
    this.isLoading.set(false);
    this.router.navigateByUrl(navigateTo);
  }

  /**
   * Shows a sanitized backend error and leaves the user on the current form.
   *
   * @param error HTTP error returned by the backend.
   * @param defaultMessage Safe fallback message for unexpected errors.
   */
  private handleMailError(error: HttpErrorResponse, defaultMessage: string): void {
    this.isLoading.set(false);
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: readApiErrorMessage(error, defaultMessage),
    });
  }

  /**
   * Sends a new mail or updates an existing draft depending on the current form mode.
   */
  onSubmit(): void {
    if (!this.validateForm()) {
      return;
    }

    const mailData = this.buildMailData();
    const attachments = this.buildAttachmentData();
    this.isLoading.set(true);

    if (this.mailData) {
      if (!this.isDraftEdit()) {
        this.showWarning('Only draft mails can be changed.');
        this.isLoading.set(false);
        return;
      }

      this.mailsService.updateMails(this.mailData.id, mailData, attachments).subscribe({
        next: () => this.handleMailSuccess('Mail updated successfully', '/mails/drafts'),
        error: (error) => this.handleMailError(error, 'Failed to update mail'),
      });
    } else {
      this.mailsService.createAndSendMail(mailData, attachments).subscribe({
        next: () => this.handleMailSuccess('Mail sent successfully', '/mails/sent'),
        error: (error) => this.handleMailError(error, 'Failed to send mail'),
      });
    }
  }

  /**
   * Saves the current form as a draft without sending it.
   */
  onSaveDraft(): void {
    if (!this.validateForm()) {
      return;
    }

    const mailData = this.buildMailData();
    const attachments = this.buildAttachmentData();
    this.isLoading.set(true);

    this.mailsService.createDraft(mailData, attachments).subscribe({
      next: () => this.handleMailSuccess('Mail saved as draft', '/mails/drafts'),
      error: (error) => this.handleMailError(error, 'Failed to save draft'),
    });
  }

  /**
   * Updates the current draft first and then sends the persisted result.
   */
  onSendDraft(): void {
    if (!this.mailData) {
      return;
    }

    if (!this.isDraftEdit()) {
      this.showWarning('Only draft mails can be sent.');
      return;
    }

    if (!this.validateForm()) {
      return;
    }

    const mailData = this.buildMailData();
    const attachments = this.buildAttachmentData();
    this.isLoading.set(true);

    this.mailsService
      .updateMails(this.mailData.id, mailData, attachments)
      .pipe(switchMap((updatedMail) => this.mailsService.sendMail(updatedMail.id)))
      .subscribe({
        next: () => this.handleMailSuccess('Draft sent successfully', '/mails/sent'),
        error: (error) => this.handleMailError(error, 'Failed to send draft'),
      });
  }

  /**
   * Restores the form to the current draft, reply template or blank creation state.
   */
  resetForm(): void {
    if (this.mailData) {
      this.fillFromExistingMail();
      this.uploadedFiles.set([]);
      return;
    }

    this.mailForm.reset({
      subject: this.replyTemplate ? this.editableReplySubject(this.replyTemplate.subject) : '',
      content: '',
      deliveryMode: this.replyTemplate ? MailDeliveryMode.EXTERNAL : MailDeliveryMode.INTERNAL,
      externalTo: this.replyTemplate?.externalTo.join(', ') ?? '',
      externalCc: this.replyTemplate?.externalCc.join(', ') ?? '',
      externalBcc: this.replyTemplate?.externalBcc.join(', ') ?? '',
      externalReplyTo: this.replyTemplate?.externalReplyTo.join(', ') ?? '',
    });
    this.clearInternalRecipients();
    this.uploadedFiles.set([]);
  }
}
