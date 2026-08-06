import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { Toast } from 'primeng/toast';
import { TicketsService } from '../../../services/tickets/tickets-service';
import {
  SupportTicket,
  SupportTicketPriority,
  SupportTicketStatus,
  TicketView,
} from '../../../types/tickets';
import { readApiErrorMessage } from '../../../utils/api-error-message';

@Component({
  selector: 'app-tickets-page',
  imports: [ProgressSpinnerModule, TagModule, Toast],
  templateUrl: './tickets-page.html',
  styleUrl: './tickets-page.css',
})
export class TicketsPage implements OnInit {
  protected view: TicketView = 'open';
  protected title = 'Open tickets';
  protected description = 'Incoming messages that still need a team response or triage.';
  protected metricLabel = 'open tickets';
  protected tickets = signal<SupportTicket[]>([]);
  protected isLoading = signal(true);

  private ticketsService = inject(TicketsService);
  private messageService = inject(MessageService);
  private router = inject(Router);

  /**
   * Configures the queue view from the current route and loads its tickets.
   */
  ngOnInit(): void {
    this.configureFromPath();
    this.loadTickets();
  }

  /**
   * Opens the detail view for a selected support ticket.
   *
   * @param ticket Ticket selected from the queue list.
   */
  openTicket(ticket: SupportTicket): void {
    this.router.navigate(['/mails/tickets', ticket.id]);
  }

  /**
   * Formats a timestamp as time for today or a compact date otherwise.
   *
   * @param dateString ISO timestamp returned by the backend.
   * @returns User-facing short date label.
   */
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    const today = new Date();

    if (date.toDateString() === today.toDateString()) {
      return date.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true });
    }

    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }

  /**
   * Builds the requester label shown on ticket cards.
   *
   * @param ticket Ticket returned by the backend.
   * @returns Requester name, email or fallback text.
   */
  requester(ticket: SupportTicket): string {
    return ticket.requesterName || ticket.requesterEmail || 'Unknown sender';
  }

  /**
   * Builds the assignment label shown on ticket cards.
   *
   * @param ticket Ticket returned by the backend.
   * @returns Assignment text for the current ticket.
   */
  assigneeLabel(ticket: SupportTicket): string {
    if (!ticket.assignedTo) {
      return 'Unassigned';
    }

    return `Assigned to ${ticket.assignedTo.firstName}`;
  }

  /**
   * Converts backend ticket status into support-team wording.
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
   * Converts backend priority into compact queue-card wording.
   *
   * @param priority Backend priority value.
   * @returns Lowercase priority label.
   */
  priorityLabel(priority: SupportTicketPriority): string {
    return priority.toLowerCase();
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
   * Maps support priority to PrimeNG severity values.
   *
   * @param priority Backend priority value.
   * @returns Severity value used by the priority tag.
   */
  prioritySeverity(priority: SupportTicketPriority): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (priority) {
      case SupportTicketPriority.URGENT:
        return 'danger';
      case SupportTicketPriority.HIGH:
        return 'warn';
      case SupportTicketPriority.LOW:
        return 'secondary';
      default:
        return 'info';
    }
  }

  /**
   * Chooses the backend queue filter and page copy from the active route.
   */
  private configureFromPath(): void {
    const path = this.router.url;
    if (path.includes('/resolved')) {
      this.view = 'resolved';
      this.title = 'Resolved tickets';
      this.description = 'Closed conversations kept visible for the whole team.';
      this.metricLabel = 'resolved tickets';
      return;
    }

    if (path.includes('/waiting')) {
      this.view = 'waiting';
      this.title = 'Waiting for sender';
      this.description = 'Tickets where the team has replied and the next response is external.';
      this.metricLabel = 'waiting tickets';
    }
  }

  /**
   * Loads tickets for the configured queue view.
   */
  private loadTickets(): void {
    this.isLoading.set(true);
    this.ticketsService.getTickets(this.view).subscribe({
      next: (tickets) => {
        this.tickets.set(tickets);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Failed to Load Tickets',
          detail: readApiErrorMessage(err),
        });
        this.isLoading.set(false);
      },
    });
  }
}
