import { Component, inject, signal, OnInit, Input, OnChanges } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  FormsModule,
} from '@angular/forms';
import { MailsService } from '../../../services/mails/mails-service';
import { MessageService } from 'primeng/api';
import { ChipModule } from 'primeng/chip';
import { ButtonModule } from 'primeng/button';
import { Router } from '@angular/router';
import { Toast } from 'primeng/toast';
import { CommonModule } from '@angular/common';
import { MultiSelectModule } from 'primeng/multiselect';
import { TextareaModule } from 'primeng/textarea';
import { FileRemoveEvent, FileSelectEvent, FileUploadModule } from 'primeng/fileupload';
import { User } from '../../../types/user';
import { CreateMail, Mail } from '../../../types/mails';
import { InputTextModule } from 'primeng/inputtext';
import { ImageModule } from 'primeng/image';
import { Attachment } from '../../../types/attachment';

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
  @Input() title: string = 'Create Mail';

  private mailsService = inject(MailsService);
  private messageService = inject(MessageService);
  private router = inject(Router);

  protected mailForm = new FormGroup({
    subject: new FormControl('', [Validators.required, Validators.maxLength(20)]),
    content: new FormControl('', [Validators.required, Validators.maxLength(500)]),
  });

  protected availableUsers = signal<User[]>([]);
  protected selectedToUsers = signal<string[]>([]);
  protected selectedCcUsers = signal<string[]>([]);
  protected selectedBccUsers = signal<string[]>([]);
  protected selectedReplyToUsers = signal<string[]>([]);
  protected uploadedFiles = signal<File[]>([]);
  protected isLoading = signal(false);
  protected attachments = signal<Attachment[]>([]);

  ngOnInit() {
    this.loadUsers();
  }

  ngOnChanges() {
    this.fillForm();
  }

  private loadUsers() {
    this.mailsService.getAllUsers().subscribe({
      next: (users) => {
        this.availableUsers.set(users);
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
    if (!this.mailData) {
      return;
    }

    this.mailForm.patchValue({
      subject: this.mailData.subject,
      content: this.mailData.content,
    });

    this.selectedToUsers.set(this.mailData.to.map((user) => user.id));
    this.selectedCcUsers.set(this.mailData.cc.map((user) => user.id));
    this.selectedBccUsers.set(this.mailData.bcc.map((user) => user.id));
    this.selectedReplyToUsers.set(this.mailData.replyTo.map((user) => user.id));

    this.attachments.set(this.mailData.attachments);
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

  private recipientsNotEmpty(): boolean {
    return (
      this.selectedToUsers().length > 0 ||
      this.selectedCcUsers().length > 0 ||
      this.selectedBccUsers().length > 0
    );
  }

  private validateForm(): boolean {
    if (this.mailForm.invalid) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Warning',
        detail: 'Please fill in all required fields',
      });
      return false;
    }

    if (!this.recipientsNotEmpty()) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Warning',
        detail: 'Please select at least one recipient',
      });
      return false;
    }

    return true;
  }

  private buildMailData(): CreateMail {
    return {
      subject: this.mailForm.get('subject')?.value || '',
      content: this.mailForm.get('content')?.value || '',
      toIds: this.selectedToUsers(),
      ccIds: this.selectedCcUsers(),
      bccIds: this.selectedBccUsers(),
      replyToIds: this.selectedReplyToUsers(),
    };
  }

  private buildAttachmentData(): File[] {
    const newAttachments = this.uploadedFiles().map((file) => file);
    const existingAttachments = this.attachments().map((att) =>
      this.blobToFile(att.blob!, att.fileName),
    );
    return [...newAttachments, ...existingAttachments];
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
    this.router.navigate([navigateTo]);
  }

  private handleMailError(error: any, defaultMessage: string) {
    this.isLoading.set(false);
    const errorMessage = error.error?.message || defaultMessage;
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: errorMessage,
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

  resetForm() {
    this.mailForm.reset();
    this.selectedToUsers.set([]);
    this.selectedCcUsers.set([]);
    this.selectedBccUsers.set([]);
    this.selectedReplyToUsers.set([]);
    this.uploadedFiles.set([]);
  }
}
