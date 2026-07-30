import type { Mail } from './mails';
import { User } from './user';

export enum SupportTicketStatus {
  OPEN = 'OPEN',
  WAITING_FOR_SUPPORT = 'WAITING_FOR_SUPPORT',
  WAITING_FOR_CUSTOMER = 'WAITING_FOR_CUSTOMER',
  RESOLVED = 'RESOLVED',
}

export enum SupportTicketPriority {
  LOW = 'LOW',
  NORMAL = 'NORMAL',
  HIGH = 'HIGH',
  URGENT = 'URGENT',
}

export type TicketView = 'open' | 'waiting' | 'resolved' | 'all';

export interface SupportTicket {
  id: string;
  ticketNumber: string;
  subject: string;
  requesterEmail?: string | null;
  requesterName?: string | null;
  status: SupportTicketStatus;
  priority: SupportTicketPriority;
  assignedTo?: User | null;
  mailCount: number;
  hasUnreadActivity: boolean;
  lastActivityAt: string;
  createdAt: string;
  updatedAt: string;
  closedAt?: string | null;
}

export interface SupportTicketDetail {
  ticket: SupportTicket;
  mails: Mail[];
}
