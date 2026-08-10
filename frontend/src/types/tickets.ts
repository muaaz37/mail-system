import type { Mail } from './mails';
import { User } from './user';

/**
 * Workflow states used by the shared support ticket queues.
 */
export enum SupportTicketStatus {
  OPEN = 'OPEN',
  WAITING_FOR_SUPPORT = 'WAITING_FOR_SUPPORT',
  WAITING_FOR_CUSTOMER = 'WAITING_FOR_CUSTOMER',
  RESOLVED = 'RESOLVED',
}

/**
 * Triage priority assigned by support users.
 */
export enum SupportTicketPriority {
  LOW = 'LOW',
  NORMAL = 'NORMAL',
  HIGH = 'HIGH',
  URGENT = 'URGENT',
}

/**
 * Queue filters supported by the ticket overview API.
 */
export type TicketView = 'open' | 'waiting' | 'resolved' | 'all';

/**
 * Ticket summary shown in queue views and returned by ticket commands.
 */
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

/**
 * Ticket detail response containing metadata and the complete mail conversation.
 */
export interface SupportTicketDetail {
  ticket: SupportTicket;
  mails: Mail[];
}
