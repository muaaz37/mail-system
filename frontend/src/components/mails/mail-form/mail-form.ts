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

  ngOnInit() {
    this.loadUsers();
    this.fillForm();
  }

  ngOnChanges() {
    this.fillForm();
  }

  protected deliveryMode(): MailDeliveryMode {
    return this.mailForm.controls.deliveryMode.value;
  }

  protected isDraftEdit(): boolean {
    return this.mailData?.status === MailStatus.DRAFT;
  }

  protected setDeliveryMode(mode: MailDeliveryMode) {
    if (this.replyTemplate && mode !== MailDeliveryMode.EXTERNAL) {
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

  private loadUsers() {
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

  private fillForm() {
    if (this.mailData) {
      this.fillFromExistingMail();
      return;
    }

    if (this.replyTemplate) {
      this.fillFromReplyTemplate();
    }
  }

  private fillFromExistingMail() {
    if (!this.mailData) {
      return;
    }

    this.mailForm.patchValue({
      subject: this.mailData.subject,
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

  private fillFromReplyTemplate() {
    if (!this.replyTemplate) {
      return;
    }

    this.mailForm.patchValue({
      subject: this.replyTemplate.subject,
      content: '',
      deliveryMode: MailDeliveryMode.EXTERNAL,
      externalTo: this.replyTemplate.externalTo.join(', '),
      externalCc: this.replyTemplate.externalCc.join(', '),
      externalBcc: this.replyTemplate.externalBcc.join(', '),
      externalReplyTo: this.replyTemplate.externalReplyTo.join(', '),
    });
    this.clearInternalRecipients();
  }

  onFileSelect(event: FileSelectEvent) {
    this.uploadedFiles.set([...this.uploadedFiles(), ...event.files]);
  }

  onFileRemove(event: FileRemoveEvent) {
    this.uploadedFiles.set(this.uploadedFiles().filter((file) => file !== event.file));
  }

  onExistingFileRemove(attachment: Attachment) {
    this.attachments.set(this.attachments().filter((att) => att.url !== attachment.url));
  }

  protected isImageAttachment(attachment: Attachment): boolean {
    return attachment.mimeType?.startsWith('image/') ?? false;
  }

  protected isPdfAttachment(attachment: Attachment): boolean {
    return attachment.mimeType === 'application/pdf' || attachment.fileName.toLowerCase().endsWith('.pdf');
  }

  private internalRecipientsNotEmpty(): boolean {
    return (
      this.selectedToUsers().length > 0 ||
      this.selectedCcUsers().length > 0 ||
      this.selectedBccUsers().length > 0
    );
  }

  private externalRecipientsNotEmpty(): boolean {
    return (
      this.parseEmailList(this.mailForm.controls.externalTo.value).length > 0 ||
      this.parseEmailList(this.mailForm.controls.externalCc.value).length > 0 ||
      this.parseEmailList(this.mailForm.controls.externalBcc.value).length > 0
    );
  }

  private validateForm(): boolean {
    if (this.mailForm.invalid) {
      this.showWarning('Please fill in all required fields');
      return false;
    }

    if (this.deliveryMode() === MailDeliveryMode.INTERNAL && !this.internalRecipientsNotEmpty()) {
      this.showWarning('Please select at least one registered user');
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

  private buildMailData(): CreateMail {
    return {
      subject: this.mailForm.controls.subject.value || '',
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

  private buildAttachmentData(): File[] {
    const newAttachments = this.uploadedFiles().map((file) => file);
    const existingAttachments = this.attachments()
      .filter((attachment) => attachment.blob)
      .map((attachment) => this.blobToFile(attachment.blob!, attachment.fileName));
    return [...newAttachments, ...existingAttachments];
  }

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

  private externalEmailsValid(): boolean {
    const emails = [
      ...this.parseEmailList(this.mailForm.controls.externalTo.value),
      ...this.parseEmailList(this.mailForm.controls.externalCc.value),
      ...this.parseEmailList(this.mailForm.controls.externalBcc.value),
      ...this.parseEmailList(this.mailForm.controls.externalReplyTo.value),
    ];
    return emails.every((email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email));
  }

  private clearInternalRecipients() {
    this.selectedToUsers.set([]);
    this.selectedCcUsers.set([]);
    this.selectedBccUsers.set([]);
    this.selectedReplyToUsers.set([]);
  }

  private clearExternalRecipients() {
    this.mailForm.patchValue({
      externalTo: '',
      externalCc: '',
      externalBcc: '',
      externalReplyTo: '',
    });
  }

  private showWarning(detail: string) {
    this.messageService.add({
      severity: 'warn',
      summary: 'Warning',
      detail,
    });
  }

  private blobToFile(blob: Blob, filename: string): File {
    return new File([blob], filename, { type: blob.type });
  }

  private handleMailSuccess(message: string, navigateTo: string) {
    this.messageService.add({
      severity: 'success',
      summary: 'Success',
      detail: message,
    });
    this.resetForm();
    this.isLoading.set(false);
    this.router.navigateByUrl(navigateTo);
  }

  private handleMailError(error: HttpErrorResponse, defaultMessage: string) {
    this.isLoading.set(false);
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: readApiErrorMessage(error, defaultMessage),
    });
  }

  onSubmit() {
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

  onSaveDraft() {
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

  onSendDraft() {
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

  resetForm() {
    if (this.mailData) {
      this.fillFromExistingMail();
      this.uploadedFiles.set([]);
      return;
    }

    this.mailForm.reset({
      subject: this.replyTemplate?.subject ?? '',
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
