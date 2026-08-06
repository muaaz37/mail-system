import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../constants';
import {
  SupportTicket,
  SupportTicketDetail,
  SupportTicketPriority,
  TicketView,
} from '../../types/tickets';

@Injectable({
  providedIn: 'root',
})
export class TicketsService {
  private http = inject(HttpClient);

  /**
   * Loads support tickets for the selected workflow view.
   *
   * @param view Queue filter requested by the user interface.
   * @returns An observable containing matching support tickets.
   */
  public getTickets(view: TicketView): Observable<SupportTicket[]> {
    return this.http.get<SupportTicket[]>(`${API_BASE_URL}/tickets`, { params: { view } });
  }

  /**
   * Loads one ticket with its complete mail conversation.
   *
   * @param id Support ticket identifier.
   * @returns An observable containing ticket metadata and related mails.
   */
  public getTicket(id: string): Observable<SupportTicketDetail> {
    return this.http.get<SupportTicketDetail>(`${API_BASE_URL}/tickets/${id}`);
  }

  /**
   * Assigns the support ticket to the authenticated user.
   *
   * @param id Support ticket identifier.
   * @returns An observable containing the updated ticket.
   */
  public assignToMe(id: string): Observable<SupportTicket> {
    return this.http.post<SupportTicket>(`${API_BASE_URL}/tickets/${id}/assign/me`, {});
  }

  /**
   * Removes the current assignee from the support ticket.
   *
   * @param id Support ticket identifier.
   * @returns An observable containing the updated ticket.
   */
  public unassign(id: string): Observable<SupportTicket> {
    return this.http.delete<SupportTicket>(`${API_BASE_URL}/tickets/${id}/assign`);
  }

  /**
   * Marks a support ticket as resolved.
   *
   * @param id Support ticket identifier.
   * @returns An observable containing the updated ticket.
   */
  public resolve(id: string): Observable<SupportTicket> {
    return this.http.post<SupportTicket>(`${API_BASE_URL}/tickets/${id}/resolve`, {});
  }

  /**
   * Moves a resolved support ticket back into the active workflow.
   *
   * @param id Support ticket identifier.
   * @returns An observable containing the updated ticket.
   */
  public reopen(id: string): Observable<SupportTicket> {
    return this.http.post<SupportTicket>(`${API_BASE_URL}/tickets/${id}/reopen`, {});
  }

  /**
   * Updates the priority used for support queue triage.
   *
   * @param id Support ticket identifier.
   * @param priority New priority selected by the user.
   * @returns An observable containing the updated ticket.
   */
  public updatePriority(id: string, priority: SupportTicketPriority): Observable<SupportTicket> {
    return this.http.put<SupportTicket>(`${API_BASE_URL}/tickets/${id}/priority`, { priority });
  }
}
