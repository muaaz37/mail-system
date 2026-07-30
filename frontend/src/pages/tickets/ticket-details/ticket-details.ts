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

  ngOnInit() {
    this.loadTicket();
  }

  goBack() {
    this.location.back();
  }

  replyToSender() {
    const mailId = this.replyMailId();
    if (mailId) {
      this.router.navigate(['/mails', mailId, 'reply']);
    }
  }

  openMail(mail: Mail) {
    this.router.navigate(['/mails', mail.id]);
  }

  assignToMe() {
    this.updateTicket(() => this.ticketsService.assignToMe(this.id), 'Ticket assigned to you');
  }

  unassign() {
    this.updateTicket(() => this.ticketsService.unassign(this.id), 'Ticket unassigned');
  }

  resolve() {
    this.updateTicket(() => this.ticketsService.resolve(this.id), 'Ticket resolved');
  }

  reopen() {
    this.updateTicket(() => this.ticketsService.reopen(this.id), 'Ticket reopened');
  }

  updatePriority(priority: SupportTicketPriority) {
    this.updateTicket(() => this.ticketsService.updatePriority(this.id, priority), 'Priority updated');
  }

  canReply(): boolean {
    return !!this.replyMailId() && this.ticket()?.status !== SupportTicketStatus.RESOLVED;
  }

  ticket(): SupportTicket | null {
    return this.detail()?.ticket ?? null;
  }

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

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString();
  }

  senderLabel(mail: Mail): string {
    if (mail.deliveryMode === MailDeliveryMode.EXTERNAL && mail.sender === null) {
      return mail.externalSenderName || mail.externalSenderEmail || 'External sender';
    }

    return getMailSenderDisplay(mail).name;
  }

  senderContext(mail: Mail): string {
    if (mail.deliveryMode === MailDeliveryMode.EXTERNAL && mail.sender === null) {
      return 'External incoming mail';
    }

    if (mail.deliveryMode === MailDeliveryMode.EXTERNAL) {
      return 'Support reply';
    }

    return 'Internal note/mail';
  }

  messagePreview(mail: Mail): string {
    const content = mail.content?.replace(/\s+/g, ' ').trim();
    if (!content) {
      return 'No text content.';
    }

    return content.length > 240 ? `${content.slice(0, 240).trim()}...` : content;
  }

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

  private loadTicket() {
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

  private replyMailId(): string | null {
    const mails = this.detail()?.mails ?? [];
    const incomingMail = [...mails]
      .reverse()
      .find((mail) => mail.status === MailStatus.RECEIVED && mail.deliveryMode === MailDeliveryMode.EXTERNAL);

    return incomingMail?.id ?? null;
  }

  private updateTicket(request: () => ReturnType<TicketsService['resolve']>, successSummary: string) {
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

  private showError(summary: string, err: HttpErrorResponse) {
    this.messageService.add({
      severity: 'error',
      summary,
      detail: readApiErrorMessage(err),
    });
  }
}
