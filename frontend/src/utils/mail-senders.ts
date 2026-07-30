import { Mail } from '../types/mails';

export interface MailSenderDisplay {
  name: string;
  email: string | null;
  initials: string;
  external: boolean;
}

export function getMailSenderDisplay(mail: Mail): MailSenderDisplay {
  if (mail.sender) {
    const name = `${mail.sender.firstName} ${mail.sender.lastName}`.trim();
    return {
      name: name || mail.sender.email,
      email: mail.sender.email,
      initials: buildInitials(name || mail.sender.email),
      external: false,
    };
  }

  const externalName = mail.externalSenderName?.trim();
  const externalEmail = mail.externalSenderEmail?.trim() || null;
  const displayName = externalName || externalEmail || 'External sender';

  return {
    name: displayName,
    email: externalEmail,
    initials: buildInitials(displayName),
    external: true,
  };
}

function buildInitials(value: string): string {
  const parts = value
    .split(/[\s@._-]+/)
    .map((part) => part.trim())
    .filter(Boolean);

  if (parts.length >= 2) {
    return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
  }

  return (parts[0] || '?').slice(0, 2).toUpperCase();
}
