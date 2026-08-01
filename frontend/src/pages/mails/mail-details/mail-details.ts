import { CommonModule, Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { ImageModule } from 'primeng/image';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { Toast } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { AuthService } from '../../../services/auth/auth-service';
import { MailsService } from '../../../services/mails/mails-service';
import { Attachment } from '../../../types/attachment';
import { Mail, MailDeliveryMode, MailStatus } from '../../../types/mails';
import { SupportTicketStatus } from '../../../types/tickets';
import { User } from '../../../types/user';
import { readApiErrorMessage } from '../../../utils/api-error-message';
import { getSeverityBadge, getSourceBadge } from '../../../utils/badges';
import { getMailSenderDisplay } from '../../../utils/mail-senders';

@Component({
  selector: 'app-mail-details',
  standalone: true,
  imports: [
    CommonModule,
    Toast,
    TagModule,
    AvatarModule,
    DividerModule,
    ProgressSpinnerModule,
    TooltipModule,
    ButtonModule,
    ImageModule,
  ],
  templateUrl: './mail-details.html',
  styleUrl: './mail-details.css',
})
export class MailDetails implements OnInit {
  @Input() protected id!: string;

  private authService = inject(AuthService);
  private mailsService = inject(MailsService);
  private messageService = inject(MessageService);
  private router = inject(Router);
  private location = inject(Location);
  private sanitizer = inject(DomSanitizer);

  protected mail = signal<Mail | null>(null);
  protected isLoading = signal(true);

  ngOnInit() {
    this.loadMail(this.id);
  }

  isUserSender(): boolean {
    const mail = this.mail();
    return mail?.sender?.id === this.authService.getCurrentUser()?.id;
  }

  canManageDraft(): boolean {
    return this.isUserSender() && this.mail()?.status === MailStatus.DRAFT;
  }

  canReplyToSupportMail(): boolean {
    const mail = this.mail();
    return mail?.status === MailStatus.RECEIVED &&
      mail.deliveryMode === MailDeliveryMode.EXTERNAL &&
      mail.ticketStatus !== SupportTicketStatus.RESOLVED;
  }

  /**
   * Determines if the current user can view the associated support ticket of the mail.
   */
  canViewTicket(): boolean {
    const mail = this.mail();
    return !!mail?.ticketId &&
      !(mail.deliveryMode === MailDeliveryMode.INTERNAL && this.isUserSender());
  }

  private loadMail(id: string) {
    this.isLoading.set(true);
    this.mailsService.getMailById(id).subscribe({
      next: (mail) => {
        mail.attachments.forEach((attachment) => this.loadAttachmentPreview(attachment));
        this.mail.set(mail);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.showError('Failed to Load Mail', err);
        this.isLoading.set(false);
      },
    });
  }

  private loadAttachmentPreview(attachment: Attachment) {
    this.mailsService.fetchAttachment(attachment.path).subscribe({
      next: (blob) => this.assignAttachmentBlob(attachment, blob),
      error: (err: HttpErrorResponse) => this.showError('Failed to Load Attachment', err),
    });
  }

  goBack() {
    this.location.back();
  }

  replyToSupportMail() {
    const mail = this.mail();
    if (mail) {
      this.router.navigate(['/mails', mail.id, 'reply']);
    }
  }

  viewTicket() {
    const ticketId = this.mail()?.ticketId;
    if (ticketId) {
      this.router.navigate(['/mails/tickets', ticketId]);
    }
  }

  ticketStatusLabel(): string {
    switch (this.mail()?.ticketStatus) {
      case SupportTicketStatus.WAITING_FOR_SUPPORT:
        return 'Waiting for team';
      case SupportTicketStatus.WAITING_FOR_CUSTOMER:
        return 'Waiting for sender';
      case SupportTicketStatus.RESOLVED:
        return 'Resolved';
      case SupportTicketStatus.OPEN:
        return 'Open';
      default:
        return '';
    }
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString();
  }

  getEmailString(recipients: User[] | undefined): string {
    if (!recipients) return '';
    return recipients.map((recipient) => this.formatUserRecipient(recipient)).join(', ');
  }

  getToSummary(mail: Mail): string {
    if (mail.deliveryMode === MailDeliveryMode.EXTERNAL) {
      return mail.externalTo.join(', ') || 'Configured support mailbox';
    }
    return mail.to.map((recipient) => recipient.firstName).join(', ');
  }

  getExternalEmailString(recipients: string[] | undefined): string {
    return recipients?.join(', ') || '';
  }

  isImageAttachment(attachment: Attachment): boolean {
    return attachment.mimeType?.startsWith('image/') ?? false;
  }

  isPdfAttachment(attachment: Attachment): boolean {
    return attachment.mimeType === 'application/pdf' || attachment.fileName?.toLowerCase().endsWith('.pdf');
  }

  editMail() {
    const mail = this.mail();
    if (mail) {
      this.router.navigate(['/mails', mail.id, 'edit']);
    }
  }

  sendMail() {
    const mail = this.mail();
    if (mail?.status === MailStatus.DRAFT) {
      this.mailsService.sendMail(mail.id).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Mail Sent',
            detail: 'The mail has been sent successfully',
          });
          this.router.navigate(['/mails/sent']);
        },
        error: (err: HttpErrorResponse) => this.showError('Failed to Send Mail', err),
      });
    }
  }

  deleteMail() {
    const mail = this.mail();
    if (mail) {
      this.mailsService.deleteMail(mail.id).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Mail Deleted',
            detail: 'The mail has been deleted successfully',
          });
          this.router.navigate(['/mails/drafts']);
        },
        error: (err: HttpErrorResponse) => this.showError('Failed to Delete Mail', err),
      });
    }
  }

  openAttachment(attachment: Attachment) {
    if (attachment.url) {
      window.open(attachment.url);
      return;
    }

    this.mailsService.fetchAttachment(attachment.path).subscribe({
      next: (blob) => {
        this.assignAttachmentBlob(attachment, blob);
        window.open(attachment.url);
      },
      error: (err: HttpErrorResponse) => this.showError('Failed to Open Attachment', err),
    });
  }

  private assignAttachmentBlob(attachment: Attachment, blob: Blob) {
    const url = URL.createObjectURL(blob);
    attachment.url = url;
    attachment.safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
    attachment.blob = blob;
  }

  private showError(summary: string, err: HttpErrorResponse) {
    this.messageService.add({
      severity: 'error',
      summary,
      detail: readApiErrorMessage(err),
    });
  }

  private formatUserRecipient(recipient: User): string {
    return `${recipient.firstName} ${recipient.lastName} (${recipient.email})`;
  }

  protected readonly getSeverityBadge = getSeverityBadge;
  protected readonly getSourceBadge = getSourceBadge;
  protected readonly getMailSenderDisplay = getMailSenderDisplay;
  protected readonly MailDeliveryMode = MailDeliveryMode;
}
