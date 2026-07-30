import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
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

  public getTickets(view: TicketView) {
    return this.http.get<SupportTicket[]>(`${API_BASE_URL}/tickets`, { params: { view } });
  }

  public getTicket(id: string) {
    return this.http.get<SupportTicketDetail>(`${API_BASE_URL}/tickets/${id}`);
  }

  public assignToMe(id: string) {
    return this.http.post<SupportTicket>(`${API_BASE_URL}/tickets/${id}/assign/me`, {});
  }

  public unassign(id: string) {
    return this.http.delete<SupportTicket>(`${API_BASE_URL}/tickets/${id}/assign`);
  }

  public resolve(id: string) {
    return this.http.post<SupportTicket>(`${API_BASE_URL}/tickets/${id}/resolve`, {});
  }

  public reopen(id: string) {
    return this.http.post<SupportTicket>(`${API_BASE_URL}/tickets/${id}/reopen`, {});
  }

  public updatePriority(id: string, priority: SupportTicketPriority) {
    return this.http.put<SupportTicket>(`${API_BASE_URL}/tickets/${id}/priority`, { priority });
  }
}
