import { Attachment } from './attachment';
import { User } from './user';

enum MailStatus {
  DRAFT = 'DRAFT',
  SENT = 'SENT',
  ERROR = 'ERROR',
}

enum MailSource{
  INTERN = 'INTERN',
  EXTERN = 'EXTERN',
}

export type Mail = {
  id: string;
  sender: User;
  subject: string;
  content: string;
  status: MailStatus;
  source: MailSource;
  to: User[];
  cc: User[];
  bcc: User[];
  replyTo: User[];
  attachments: Attachment[];
  createdAt: string;
  updatedAt: string;
  sentAt?: string;
};

export type CreateMail = {
  subject: string;
  content: string;
  toIds: string[];
  ccIds: string[];
  bccIds: string[];
  replyToIds: string[];
};

export type UpdateMail = CreateMail;
