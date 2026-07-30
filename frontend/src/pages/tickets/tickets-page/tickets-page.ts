import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
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

  ngOnInit() {
    this.configureFromPath();
    this.loadTickets();
  }

  openTicket(ticket: SupportTicket) {
    this.router.navigate(['/mails/tickets', ticket.id]);
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    const today = new Date();

    if (date.toDateString() === today.toDateString()) {
      return date.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true });
    }

    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }

  requester(ticket: SupportTicket): string {
    return ticket.requesterName || ticket.requesterEmail || 'Unknown sender';
  }

  assigneeLabel(ticket: SupportTicket): string {
    if (!ticket.assignedTo) {
      return 'Unassigned';
    }

    return `Assigned to ${ticket.assignedTo.firstName}`;
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

  priorityLabel(priority: SupportTicketPriority): string {
    return priority.toLowerCase();
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

  private configureFromPath() {
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

  private loadTickets() {
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
