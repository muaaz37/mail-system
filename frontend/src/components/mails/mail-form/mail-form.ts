import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, Input, OnChanges, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
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
import {
  buildAttachmentFiles,
  MailFormAttachments,
} from './attachments/mail-form-attachments';
import {
  areEmailAddressesValid,
  buildReplySubject,
  mapMailFormToCreateMail,
  normalizeReplySubject,
  parseEmailAddresses,
} from './mapping/mail-form.mapper';
import { MailFormRecipients } from './recipients/mail-form-recipients';

@Component({
  selector: 'app-mail-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    TextareaModule,
    Toast,
    InputTextModule,
    MailFormRecipients,
    MailFormAttachments,
  ],
  templateUrl: './mail-form.html',
})
export class MailForm implements OnInit, OnChanges {
  @Input() mailData: Mail | null = null;
  @Input() replyTemplate: MailReplyTemplate | null = null;
  @Input() title = 'Create Mail';

  private readonly mailsService = inject(MailsService);
  private readonly messageService = inject(MessageService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

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
  protected readonly acceptedAttachmentTypes =
    'application/pdf,image/png,image/jpeg,image/gif,image/webp,.pdf,.png,.jpg,.jpeg,.gif,.webp';
  protected readonly maxAttachmentSizeBytes = 10 * 1024 * 1024;
  protected readonly maxAttachmentSizeLabel = '10 MB';
  protected readonly MailDeliveryMode = MailDeliveryMode;

  /**
   * Returns whether the form represents a new or persisted reply.
   *
   * @returns True when reply-specific form behavior must be applied.
   */
  protected isReply(): boolean {
    return this.replyTemplate !== null || !!this.mailData?.replyToMailId || this.hasStoredTicketContext();
  }

  /**
   * Returns the immutable ticket number of an external support reply.
   *
   * @returns Ticket number for an external reply or null otherwise.
   */
  protected replyTicketNumber(): string | null {
    const template = this.replyTemplate;

    if (template?.deliveryMode === MailDeliveryMode.EXTERNAL) {
      return template.ticketNumber;
    }

    return this.mailData?.ticketNumber ?? null;
  }

  /**
   * Returns whether the form is bound to an external support reply.
   *
   * @returns True when a ticket number must be kept in the outgoing subject.
   */
  protected isSupportReply(): boolean {
    return (
      this.replyTemplate?.deliveryMode === MailDeliveryMode.EXTERNAL ||
      this.hasStoredTicketContext()
    );
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
   * Formats the locked primary recipients of an internal reply.
   *
   * @returns Comma-separated email addresses for the selected internal users.
   */
  protected internalToDisplayValue(): string {
    const selectedIds = new Set(this.selectedToUsers());

    return this.availableUsers()
      .filter((user) => selectedIds.has(user.id))
      .map((user) => user.email)
      .join(', ');
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
   * Changes the delivery mode for a regular mail.
   *
   * Replies retain the delivery channel of their original mail because the
   * recipient context is generated and validated by the backend.
   *
   * @param mode Delivery mode selected by the user.
   */
  protected setDeliveryMode(mode: MailDeliveryMode): void {
    const lockedReplyMode =
      this.replyTemplate?.deliveryMode ??
      (
        this.mailData?.replyToMailId
          ? this.mailData.deliveryMode
          : null
      );

    if (lockedReplyMode && mode !== lockedReplyMode) {
      this.showWarning('The delivery mode cannot be changed for a reply.');
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
   * Initializes the form from trusted reply metadata returned by the backend.
   *
   * Internal and external templates are handled separately so that each
   * delivery channel receives only its applicable recipient values.
   */
  private fillFromReplyTemplate(): void {
    const template = this.replyTemplate;

    if (!template) {
      return;
    }

    this.mailForm.reset({
      subject: this.editableReplySubject(template.subject),
      content: '',
      deliveryMode: template.deliveryMode,
      externalTo: '',
      externalCc: '',
      externalBcc: '',
      externalReplyTo: '',
    });

    this.selectedToUsers.set([]);
    this.selectedCcUsers.set([]);
    this.selectedBccUsers.set([]);
    this.selectedReplyToUsers.set([]);

    if (template.deliveryMode === MailDeliveryMode.INTERNAL) {
      this.selectedToUsers.set(template.recipientIds);
      return;
    }

    this.mailForm.controls.externalTo.setValue(template.recipients.join(', '));
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
      parseEmailAddresses(this.mailForm.controls.externalTo.value).length > 0 ||
      parseEmailAddresses(this.mailForm.controls.externalCc.value).length > 0 ||
      parseEmailAddresses(this.mailForm.controls.externalBcc.value).length > 0
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
    const formValues = this.mailForm.getRawValue();

    return mapMailFormToCreateMail(
      {
        ...formValues,
        subject: this.completeSubject(),
        content: formValues.content ?? '',
      },
      {
        toIds: this.selectedToUsers(),
        ccIds: this.selectedCcUsers(),
        bccIds: this.selectedBccUsers(),
        replyToIds: this.selectedReplyToUsers(),
      },
      this.replyTemplate?.replyToMailId ?? this.mailData?.replyToMailId ?? null,
    );
  }

  /**
   * Builds the full attachment file list sent to the backend replacement endpoint.
   *
   * @returns New uploads plus retained existing attachments represented as File objects.
   */
  private buildAttachmentData(): File[] {
    return buildAttachmentFiles(this.uploadedFiles(), this.attachments());
  }

  /**
   * Normalizes the editable subject of a reply.
   *
   * Ticket numbers and repeated reply prefixes are removed before exactly one
   * reply prefix is restored.
   *
   * @param subject Subject returned by the backend or loaded from a draft.
   * @returns Editable reply subject.
   */
  private editableReplySubject(subject: string): string {
    return this.isReply() ? normalizeReplySubject(subject) : subject;
  }

  /**
   * Builds the final subject sent to the backend.
   *
   * External support replies retain their immutable ticket number. Internal
   * replies use only the normalized reply subject.
   *
   * @returns Complete subject for the current mail.
   */
  protected completeSubject(): string {
    const subject = this.mailForm.controls.subject.value?.trim() ?? '';
    return buildReplySubject(subject, this.replyTicketNumber());
  }

  /**
   * Detects persisted support reply drafts where no fresh reply template is available.
   *
   * @returns True when an existing external draft is already linked to a support ticket.
   */
  private hasStoredTicketContext(): boolean {
    return (
      this.mailData?.deliveryMode === MailDeliveryMode.EXTERNAL && !!this.mailData.ticketNumber
    );
  }

  /**
   * Validates all entered external email addresses with a conservative browser-side check.
   *
   * @returns True when every parsed external address has a basic email shape.
   */
  private externalEmailsValid(): boolean {
    const emails = [
      ...parseEmailAddresses(this.mailForm.controls.externalTo.value),
      ...parseEmailAddresses(this.mailForm.controls.externalCc.value),
      ...parseEmailAddresses(this.mailForm.controls.externalBcc.value),
      ...parseEmailAddresses(this.mailForm.controls.externalReplyTo.value),
    ];
    return areEmailAddressesValid(emails);
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
   */
  private showWarning(detail: string): void {
    this.messageService.add({
      severity: 'warn',
      summary: 'Warning',
      detail,
    });
  }

  /**
   * Shows a success toast, resets local form state and navigates to the target mailbox view.
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

      this.mailsService.updateMail(this.mailData.id, mailData, attachments).subscribe({
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
      .updateMail(this.mailData.id, mailData, attachments)
      .pipe(switchMap((updatedMail) => this.mailsService.sendMail(updatedMail.id)))
      .subscribe({
        next: () => this.handleMailSuccess('Draft sent successfully', '/mails/sent'),
        error: (error) => this.handleMailError(error, 'Failed to send draft'),
      });
  }

  /**
   * Restores the form to its initial draft, reply or compose state.
   */
  resetForm(): void {
    this.uploadedFiles.set([]);

    if (this.mailData) {
      this.fillFromExistingMail();
      return;
    }

    if (this.replyTemplate) {
      this.fillFromReplyTemplate();
      this.attachments.set([]);
      return;
    }

    this.mailForm.reset({
      subject: '',
      content: '',
      deliveryMode: MailDeliveryMode.INTERNAL,
      externalTo: '',
      externalCc: '',
      externalBcc: '',
      externalReplyTo: '',
    });

    this.clearInternalRecipients();
    this.attachments.set([]);
  }
}
