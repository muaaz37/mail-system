import { Attachment } from './attachment';
import { SupportTicketPriority, SupportTicketStatus } from './tickets';
import { User } from './user';

export enum MailStatus {
  DRAFT = 'DRAFT',
  SENT = 'SENT',
  ERROR = 'ERROR',
  RECEIVED = 'RECEIVED',
}

export enum MailSource {
  INTERN = 'INTERN',
  EXTERN = 'EXTERN',
}

export enum MailDeliveryMode {
  INTERNAL = 'INTERNAL',
  EXTERNAL = 'EXTERNAL',
}

export interface Mail {
  id: string;
  sender: User | null;
  externalSenderEmail?: string | null;
  externalSenderName?: string | null;
  externalMessageId?: string | null;
  externalSentAt?: string | null;
  ticketNumber?: string | null;
  ticketId?: string | null;
  ticketStatus?: SupportTicketStatus | null;
  ticketPriority?: SupportTicketPriority | null;
  ticketAssignedTo?: User | null;
  subject: string;
  content: string;
  status: MailStatus;
  source: MailSource;
  deliveryMode: MailDeliveryMode;
  to: User[];
  cc: User[];
  bcc: User[];
  replyTo: User[];
  externalTo: string[];
  externalCc: string[];
  externalBcc: string[];
  externalReplyTo: string[];
  attachments: Attachment[];
  createdAt: string;
  updatedAt: string;
  sentAt?: string;
}

export interface CreateMail {
  subject: string;
  content: string;
  deliveryMode: MailDeliveryMode;
  toIds: string[];
  ccIds: string[];
  bccIds: string[];
  replyToIds: string[];
  externalTo: string[];
  externalCc: string[];
  externalBcc: string[];
  externalReplyTo: string[];
  replyToMailId?: string | null;
}

export interface UpdateMail {
  subject: string;
  content: string;
  deliveryMode: MailDeliveryMode;
  toIds: string[];
  ccIds: string[];
  bccIds: string[];
  replyToIds: string[];
  externalTo: string[];
  externalCc: string[];
  externalBcc: string[];
  externalReplyTo: string[];
  replyToMailId?: string | null;
}

export interface MailReplyTemplate {
  replyToMailId: string;
  ticketNumber: string;
  subject: string;
  externalTo: string[];
  externalCc: string[];
  externalBcc: string[];
  externalReplyTo: string[];
}
