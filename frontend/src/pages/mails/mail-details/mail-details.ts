import { CommonModule, Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { Toast } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { AuthService } from '../../../services/auth/auth-service';
import { MailsService } from '../../../services/mails/mails-service';
import { Mail, MailDeliveryMode, MailStatus } from '../../../types/mails';
import { SupportTicketStatus } from '../../../types/tickets';
import { User } from '../../../types/user';
import { readApiErrorMessage } from '../../../utils/api-error-message';
import { getSeverityBadge, getSourceBadge } from '../../../utils/badges';
import { getMailSenderDisplay } from '../../../utils/mail-senders';
import { MailAttachmentList } from './attachments/mail-attachment-list';
import { MailConversation } from './conversation/mail-conversation';

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
    MailAttachmentList,
    MailConversation,
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

  protected mail = signal<Mail | null>(null);
  protected isLoading = signal(true);
  protected isConversationVisible = signal(false);

  /**
   * Loads the selected mail when the route parameter has been bound.
   */
  ngOnInit(): void {
    this.loadMail(this.id);
  }

  /**
   * Checks whether the authenticated user created the displayed mail.
   *
   * @returns True when the current user is the mail sender.
   */
  isUserSender(): boolean {
    const mail = this.mail();
    return mail?.sender?.id === this.authService.getCurrentUser()?.id;
  }

  /**
   * Checks whether draft actions should be visible for the displayed mail.
   *
   * @returns True when the current user may edit, send or delete the draft.
   */
  canManageDraft(): boolean {
    return this.isUserSender() && this.mail()?.status === MailStatus.DRAFT;
  }

  /**
   * Checks whether the displayed mail can be answered by the current user.
   *
   * Internal inbox mails can be answered after delivery. External support mails
   * can be answered while their associated ticket remains unresolved.
   *
   * @returns True when the reply action should be available.
   */
  canReply(): boolean {
    const mail = this.mail();

    if (!mail || this.isUserSender()) {
      return false;
    }

    if (mail.deliveryMode === MailDeliveryMode.INTERNAL) {
      return mail.status === MailStatus.SENT && mail.sender !== null;
    }

    return (
      mail.status === MailStatus.RECEIVED && mail.ticketStatus !== SupportTicketStatus.RESOLVED
    );
  }

  /**
   * Determines if the current user can view the associated support ticket of the mail.
   *
   * @returns True when a ticket exists and the mail is not just the user's internal sent copy.
   */
  canViewTicket(): boolean {
    const mail = this.mail();
    return mail?.deliveryMode === MailDeliveryMode.EXTERNAL && !!mail.ticketId;
  }

  /** Returns whether the internal conversation action is available. */
  canViewConversation(): boolean {
    const mail = this.mail();
    return mail?.deliveryMode === MailDeliveryMode.INTERNAL && mail.status === MailStatus.SENT;
  }

  /** Toggles the internal conversation section. */
  toggleConversation(): void {
    if (this.canViewConversation()) {
      this.isConversationVisible.update((visible) => !visible);
    }
  }

  protected handleConversationError(error: HttpErrorResponse): void {
    this.isConversationVisible.set(false);
    this.showError('Failed to Load Conversation', error);
  }

  /**
   * Loads one mail and preloads attachment blobs for inline previews.
   *
   * @param id Mail identifier from the route.
   */
  private loadMail(id: string): void {
    this.isLoading.set(true);
    this.isConversationVisible.set(false);
    this.mailsService.getMailById(id).subscribe({
      next: (mail) => {
        this.mail.set(mail);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.showError('Failed to Load Mail', err);
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Navigates back to the previous browser history entry.
   */
  goBack(): void {
    this.location.back();
  }

  /**
   * Opens the compose page with reply metadata for the displayed mail.
   */
  replyToMail(): void {
    const mail = this.mail();

    if (!mail || !this.canReply()) {
      return;
    }

    this.router.navigate(['/mails', mail.id, 'reply']);
  }

  /**
   * Opens the support ticket connected to the displayed mail.
   */
  viewTicket(): void {
    const ticketId = this.mail()?.ticketId;
    if (ticketId) {
      this.router.navigate(['/mails/tickets', ticketId]);
    }
  }

  /**
   * Converts the backend ticket status into user-facing copy.
   *
   * @returns Label displayed in the mail detail header.
   */
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

  /**
   * Formats an ISO timestamp for display in the user's locale.
   *
   * @param dateString ISO timestamp returned by the backend.
   * @returns Localized date and time string.
   */
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString();
  }

  /**
   * Formats internal recipients as readable name and email pairs.
   *
   * @param recipients Internal user recipients, if present.
   * @returns Comma-separated recipient labels.
   */
  getEmailString(recipients: User[] | undefined): string {
    if (!recipients) return '';
    return recipients.map((recipient) => this.formatUserRecipient(recipient)).join(', ');
  }

  /**
   * Builds the primary recipient summary for internal and external mails.
   *
   * @param mail Mail shown in the details view.
   * @returns User-facing recipient summary.
   */
  getToSummary(mail: Mail): string {
    if (mail.deliveryMode === MailDeliveryMode.EXTERNAL) {
      return mail.externalTo.join(', ') || 'Configured support mailbox';
    }
    return mail.to.map((recipient) => recipient.firstName).join(', ');
  }

  /**
   * Formats external recipient addresses.
   *
   * @param recipients External email recipients, if present.
   * @returns Comma-separated email addresses.
   */
  getExternalEmailString(recipients: string[] | undefined): string {
    return recipients?.join(', ') || '';
  }

  /**
   * Opens the draft edit page for the displayed mail.
   */
  editMail(): void {
    const mail = this.mail();
    if (mail) {
      this.router.navigate(['/mails', mail.id, 'edit']);
    }
  }

  /**
   * Sends the displayed draft mail and navigates to the sent mailbox on success.
   */
  sendMail(): void {
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

  /**
   * Deletes the displayed mail and returns to the draft mailbox on success.
   */
  deleteMail(): void {
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

  /**
   * Displays a sanitized API error in a toast.
   *
   * @param summary Short error title shown to the user.
   * @param err HTTP error returned by the backend.
   */
  private showError(summary: string, err: HttpErrorResponse): void {
    this.messageService.add({
      severity: 'error',
      summary,
      detail: readApiErrorMessage(err),
    });
  }

  /**
   * Formats one internal user recipient for detail rows.
   *
   * @param recipient User recipient returned by the backend.
   * @returns Full name with email address.
   */
  private formatUserRecipient(recipient: User): string {
    return `${recipient.firstName} ${recipient.lastName} (${recipient.email})`;
  }

  protected readonly getSeverityBadge = getSeverityBadge;
  protected readonly getSourceBadge = getSourceBadge;
  protected readonly getMailSenderDisplay = getMailSenderDisplay;
  protected readonly MailDeliveryMode = MailDeliveryMode;
}
