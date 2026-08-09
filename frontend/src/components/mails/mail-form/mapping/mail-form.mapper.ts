import { CreateMail, MailDeliveryMode } from '../../../../types/mails';

/** Extracts unique addresses from free-form recipient input. */
export function parseEmailAddresses(value: string | null): string[] {
  const input = value ?? '';
  const namedAddresses = [...input.matchAll(/<([^<>\s]+@[^<>\s]+)>/g)].map(
    (match) => match[1],
  );
  const plainAddresses = input
    .replace(/[^,;]*<[^<>]+>/g, ' ')
    .split(/[;,\s]+/)
    .map((email) => email.trim())
    .filter(Boolean);

  return [...new Set([...namedAddresses, ...plainAddresses])];
}

/** Returns whether every supplied address has a valid basic email shape. */
export function areEmailAddressesValid(addresses: readonly string[]): boolean {
  return addresses.every((email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email));
}

/** Removes ticket markers and repeated reply prefixes from a reply subject. */
export function normalizeReplySubject(subject: string): string {
  const withoutTicketNumbers = subject.replace(/\[?\s*TICKET-\d+\s*]?/gi, ' ').trim();
  const baseSubject = withoutTicketNumbers.replace(/^(?:\s*Re\s*:\s*)+/i, '').trim();
  return `Re: ${baseSubject}`.trim();
}

/** Adds the immutable ticket number to a normalized support reply subject. */
export function buildReplySubject(subject: string, ticketNumber: string | null): string {
  const trimmedSubject = subject.trim();
  return ticketNumber
    ? `[${ticketNumber}] ${normalizeReplySubject(trimmedSubject)}`.trim()
    : trimmedSubject;
}

/**
 * Maps the form state to the delivery-mode-specific backend command.
 */
export interface MailFormValues {
  subject: string;
  content: string;
  deliveryMode: MailDeliveryMode;
  externalTo: string | null;
  externalCc: string | null;
  externalBcc: string | null;
  externalReplyTo: string | null;
}

/**
 * Maps the internal recipient selection to the delivery-mode-specific backend command.
 */
export interface InternalRecipientSelection {
  toIds: readonly string[];
  ccIds: readonly string[];
  bccIds: readonly string[];
  replyToIds: readonly string[];
}

/** Maps the form state to the delivery-mode-specific backend command. */
export function mapMailFormToCreateMail(
  values: MailFormValues,
  internalRecipients: InternalRecipientSelection,
  replyToMailId: string | null,
): CreateMail {
  const isInternal = values.deliveryMode === MailDeliveryMode.INTERNAL;

  return {
    subject: values.subject,
    content: values.content,
    deliveryMode: values.deliveryMode,
    toIds: isInternal ? [...internalRecipients.toIds] : [],
    ccIds: isInternal ? [...internalRecipients.ccIds] : [],
    bccIds: isInternal ? [...internalRecipients.bccIds] : [],
    replyToIds: isInternal ? [...internalRecipients.replyToIds] : [],
    externalTo: isInternal ? [] : parseEmailAddresses(values.externalTo),
    externalCc: isInternal ? [] : parseEmailAddresses(values.externalCc),
    externalBcc: isInternal ? [] : parseEmailAddresses(values.externalBcc),
    externalReplyTo: isInternal ? [] : parseEmailAddresses(values.externalReplyTo),
    replyToMailId,
  };
}
