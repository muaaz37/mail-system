import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { Toast } from 'primeng/toast';
import { EMPTY, catchError, exhaustMap, finalize, timeout, timer } from 'rxjs';
import { MAILBOX_REFRESH_INTERVAL_MS, MAILBOX_REFRESH_TIMEOUT_MS } from '../../../constants';
import { TicketsService } from '../../../services/tickets/tickets-service';
import {
  SupportTicket,
  SupportTicketPriority,
  SupportTicketStatus,
  TicketView,
} from '../../../types/tickets';
import { User } from '../../../types/user';
import { readApiErrorMessage } from '../../../utils/api-error-message';

interface TicketListItem {
  id: string;
  type: 'ticket';
  subject: string;
  requesterName: string;
  lastActivityAt: string;
  mailCount: number;
  hasUnreadActivity: boolean;
  ticketNumber?: string;
  status?: SupportTicketStatus;
  priority?: SupportTicketPriority;
  assignedTo?: User | null;
}

@Component({
  selector: 'app-tickets-page',
  imports: [ProgressSpinnerModule, TagModule, Toast],
  templateUrl: './tickets-page.html',
  styleUrl: './tickets-page.css',
})
export class TicketsPage implements OnInit {
  protected view: TicketView = 'open';
  protected title = 'Open tickets';
  protected description =
    'External support tickets that need attention.';
  protected metricLabel = 'open items';

  protected items = signal<TicketListItem[]>([]);
  protected isLoading = signal(true);

  private readonly ticketsService = inject(TicketsService);
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private hasReportedLoadError = false;

  /**
   * Configures the queue view from the current route and loads its messages.
   */
  ngOnInit(): void {
    this.configureFromPath();
    this.startAutoRefresh();
  }

  /**
   * Opens the detail view for a selected support ticket.
   *
   * @param item Queue item selected from the overview.
   */
  protected openItem(item: TicketListItem): void {
    this.router.navigate(['/mails/tickets', item.id]);
  }

  /**
   * Formats a timestamp as time for today or a compact date otherwise.
   *
   * @param dateString ISO timestamp returned by the backend.
   * @returns User-facing short date label.
   */
  protected formatDate(dateString: string): string {
    const date = new Date(dateString);
    const today = new Date();

    if (date.toDateString() === today.toDateString()) {
      return date.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true,
      });
    }

    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    });
  }

  /**
   * Builds the assignment label shown on ticket cards.
   *
   * @param item Ticket queue item.
   * @returns Assignment text for the current ticket.
   */
  protected assigneeLabel(item: TicketListItem): string {
    if (!item.assignedTo) {
      return 'Unassigned';
    }

    return `Assigned to ${item.assignedTo.firstName}`;
  }

  /**
   * Converts backend ticket status into support-team wording.
   *
   * @param status Backend ticket status.
   * @returns Display label for the status tag.
   */
  protected statusLabel(status: SupportTicketStatus): string {
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
  protected priorityLabel(priority: SupportTicketPriority): string {
    return priority.toLowerCase();
  }

  /**
   * Maps ticket status to PrimeNG severity values.
   *
   * @param status Backend ticket status.
   * @returns Severity value used by the status tag.
   */
  protected statusSeverity(
    status: SupportTicketStatus,
  ): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
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
  protected prioritySeverity(
    priority: SupportTicketPriority,
  ): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
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
      this.description =
        'Closed conversations kept visible for the whole team.';
      this.metricLabel = 'resolved tickets';
      return;
    }

    if (path.includes('/waiting')) {
      this.view = 'waiting';
      this.title = 'Waiting for sender';
      this.description =
        'Tickets where the team has replied and the next response is external.';
      this.metricLabel = 'waiting tickets';
    }
  }

  /**
   * Starts periodic list refresh so server-side IMAP imports become visible without a browser reload.
   */
  private startAutoRefresh(): void {
    timer(0, MAILBOX_REFRESH_INTERVAL_MS)
      .pipe(
        exhaustMap((cycle) => {
          const initialLoad = cycle === 0 && this.items().length === 0;
          this.isLoading.set(initialLoad);

          return this.ticketsService.getTickets(this.view).pipe(
            timeout({ first: MAILBOX_REFRESH_TIMEOUT_MS }),
            catchError((err: unknown) => {
              this.handleLoadError(err);
              return EMPTY;
            }),
            finalize(() => {
              this.isLoading.set(false);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((tickets) => this.updateItems(tickets));
  }

  /**
   * Replaces the current queue with the latest backend state.
   *
   * @param tickets Support tickets returned by the backend.
   */
  private updateItems(tickets: SupportTicket[]): void {
    const ticketItems = tickets.map((ticket) => this.mapTicket(ticket)).sort(
      (first, second) =>
        new Date(second.lastActivityAt).getTime() -
        new Date(first.lastActivityAt).getTime(),
    );

    this.items.set(ticketItems);
    this.hasReportedLoadError = false;
  }

  /**
   * Shows one sanitized load error until a later refresh succeeds.
   */
  private handleLoadError(err: unknown): void {
    if (this.hasReportedLoadError) {
      return;
    }

    this.messageService.add({
      severity: 'error',
      summary: 'Failed to Load Messages',
      detail: readApiErrorMessage(err),
    });
    this.hasReportedLoadError = true;
  }

  /**
   * Converts a support ticket into the common queue item representation.
   *
   * @param ticket Support ticket returned by the backend.
   * @returns Queue item displayed by the ticket overview.
   */
  private mapTicket(ticket: SupportTicket): TicketListItem {
    return {
      id: ticket.id,
      type: 'ticket',
      ticketNumber: ticket.ticketNumber,
      subject: ticket.subject,
      requesterName:
        ticket.requesterName ||
        ticket.requesterEmail ||
        'Unknown sender',
      status: ticket.status,
      priority: ticket.priority,
      assignedTo: ticket.assignedTo,
      mailCount: ticket.mailCount,
      hasUnreadActivity: ticket.hasUnreadActivity,
      lastActivityAt: ticket.lastActivityAt,
    };
  }

}
