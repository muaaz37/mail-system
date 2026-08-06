import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { Toast } from 'primeng/toast';
import { TicketsService } from '../../../services/tickets/tickets-service';
import { Mail, MailDeliveryMode, MailStatus } from '../../../types/mails';
import {
  SupportTicket,
  SupportTicketDetail,
  SupportTicketPriority,
  SupportTicketStatus,
} from '../../../types/tickets';
import { readApiErrorMessage } from '../../../utils/api-error-message';
import { getMailSenderDisplay } from '../../../utils/mail-senders';

@Component({
  selector: 'app-ticket-details',
  imports: [ProgressSpinnerModule, TagModule, Toast],
  templateUrl: './ticket-details.html',
  styleUrl: './ticket-details.css',
})
export class TicketDetails implements OnInit {
  @Input() protected id!: string;

  protected detail = signal<SupportTicketDetail | null>(null);
  protected isLoading = signal(true);
  protected readonly SupportTicketStatus = SupportTicketStatus;
  protected readonly SupportTicketPriority = SupportTicketPriority;
  protected readonly priorities = Object.values(SupportTicketPriority);

  private ticketsService = inject(TicketsService);
  private messageService = inject(MessageService);
  private router = inject(Router);
  private location = inject(Location);

  /**
   * Loads the selected ticket and lets the backend mark it as read for the current user.
   */
  ngOnInit(): void {
    this.loadTicket();
  }

  /**
   * Navigates back to the previous browser history entry.
   */
  goBack(): void {
    this.location.back();
  }

  /**
   * Opens the compose view for the latest external incoming mail in this ticket.
   */
  replyToSender(): void {
    const mailId = this.replyMailId();
    if (mailId) {
      this.router.navigate(['/mails', mailId, 'reply']);
    }
  }

  /**
   * Opens a mail from the ticket conversation in the mail detail view.
   *
   * @param mail Conversation mail selected by the user.
   */
  openMail(mail: Mail): void {
    this.router.navigate(['/mails', mail.id]);
  }

  /**
   * Assigns the displayed ticket to the authenticated user.
   */
  assignToMe(): void {
    this.updateTicket(() => this.ticketsService.assignToMe(this.id), 'Ticket assigned to you');
  }

  /**
   * Removes the current assignee from the displayed ticket.
   */
  unassign(): void {
    this.updateTicket(() => this.ticketsService.unassign(this.id), 'Ticket unassigned');
  }

  /**
   * Marks the displayed ticket as resolved.
   */
  resolve(): void {
    this.updateTicket(() => this.ticketsService.resolve(this.id), 'Ticket resolved');
  }

  /**
   * Reopens the displayed ticket for further support work.
   */
  reopen(): void {
    this.updateTicket(() => this.ticketsService.reopen(this.id), 'Ticket reopened');
  }

  /**
   * Updates the triage priority of the displayed ticket.
   *
   * @param priority New priority selected by the user.
   */
  updatePriority(priority: SupportTicketPriority): void {
    this.updateTicket(() => this.ticketsService.updatePriority(this.id, priority), 'Priority updated');
  }

  /**
   * Checks whether the user may answer the ticket from the current state.
   *
   * @returns True when there is an incoming mail and the ticket is not resolved.
   */
  canReply(): boolean {
    return !!this.replyMailId() && this.ticket()?.status !== SupportTicketStatus.RESOLVED;
  }

  /**
   * Returns the currently displayed ticket metadata.
   *
   * @returns Ticket metadata, or null while the ticket is loading.
   */
  ticket(): SupportTicket | null {
    return this.detail()?.ticket ?? null;
  }

  /**
   * Converts ticket workflow status into user-facing copy.
   *
   * @param status Backend ticket status.
   * @returns Display label for the status tag.
   */
  statusLabel(status: SupportTicketStatus): string {
    switch (status) {
      case SupportTicketStatus.WAITING_FOR_SUPPORT:
        return 'Waiting for team';
      case SupportTicketStatus.WAITING_FOR_CUSTOMER:
        return 'Waiting for sender';
      case SupportTicketStatus.RESOLVED:
        return 'Resolved';
      default:
        return 'Open';
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
   * Builds the sender label for internal mails, support replies and external incoming mails.
   *
   * @param mail Conversation mail returned with the ticket.
   * @returns Display name for the message author.
   */
  senderLabel(mail: Mail): string {
    if (mail.deliveryMode === MailDeliveryMode.EXTERNAL && mail.sender === null) {
      return mail.externalSenderName || mail.externalSenderEmail || 'External sender';
    }

    return getMailSenderDisplay(mail).name;
  }

  /**
   * Describes how a conversation mail entered the support workflow.
   *
   * @param mail Conversation mail returned with the ticket.
   * @returns Short context label for the message.
   */
  senderContext(mail: Mail): string {
    if (mail.deliveryMode === MailDeliveryMode.EXTERNAL && mail.sender === null) {
      return 'External incoming mail';
    }

    if (mail.deliveryMode === MailDeliveryMode.EXTERNAL) {
      return 'Support reply';
    }

    return 'Internal note/mail';
  }

  /**
   * Builds a compact preview from a conversation mail body.
   *
   * @param mail Conversation mail shown in the ticket timeline.
   * @returns Shortened text preview.
   */
  messagePreview(mail: Mail): string {
    const content = mail.content?.replace(/\s+/g, ' ').trim();
    if (!content) {
      return 'No text content.';
    }

    return content.length > 240 ? `${content.slice(0, 240).trim()}...` : content;
  }

  /**
   * Maps ticket status to PrimeNG severity values.
   *
   * @param status Backend ticket status.
   * @returns Severity value used by the status tag.
   */
  statusSeverity(status: SupportTicketStatus): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (status) {
      case SupportTicketStatus.RESOLVED:
        return 'success';
      case SupportTicketStatus.WAITING_FOR_CUSTOMER:
        return 'warn';
      case SupportTicketStatus.WAITING_FOR_SUPPORT:
        return 'danger';
      default:
        return 'info';
    }
  }

  /**
   * Loads ticket metadata and conversation mails from the backend.
   */
  private loadTicket(): void {
    this.isLoading.set(true);
    this.ticketsService.getTicket(this.id).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.showError('Failed to Load Ticket', err);
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Finds the latest external incoming mail that can be used as reply context.
   *
   * @returns Mail identifier used for reply routing, or null when no reply target exists.
   */
  private replyMailId(): string | null {
    const mails = this.detail()?.mails ?? [];
    const incomingMail = [...mails]
      .reverse()
      .find((mail) => mail.status === MailStatus.RECEIVED && mail.deliveryMode === MailDeliveryMode.EXTERNAL);

    return incomingMail?.id ?? null;
  }

  /**
   * Executes one ticket command and updates the local detail signal with the returned ticket state.
   *
   * @param request Deferred service call for the selected ticket command.
   * @param successSummary Toast summary shown after a successful command.
   */
  private updateTicket(request: () => ReturnType<TicketsService['resolve']>, successSummary: string): void {
    request().subscribe({
      next: (ticket) => {
        const current = this.detail();
        if (current) {
          this.detail.set({ ...current, ticket });
        }
        this.messageService.add({ severity: 'success', summary: successSummary });
      },
      error: (err: HttpErrorResponse) => this.showError(successSummary, err),
    });
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
}
