import { Component, Input, inject } from '@angular/core';
import { Router } from '@angular/router';
import { TagModule } from 'primeng/tag';
import { Mail, MailDeliveryMode } from '../../../types/mails';
import { getSeverityBadge } from '../../../utils/badges';
import { getMailSenderDisplay } from '../../../utils/mail-senders';

@Component({
  selector: 'app-mails-list-element',
  imports: [TagModule],
  templateUrl: './mails-list-element.html',
  styleUrl: './mails-list-element.css',
})
export class MailsListElement {
  @Input() mail!: Mail;
  @Input() mailbox: 'inbox' | 'sent' | 'drafts' = 'inbox';

  private readonly router = inject(Router);
  protected readonly getSeverityBadge = getSeverityBadge;
  protected readonly getMailSenderDisplay = getMailSenderDisplay;

  /**
   * Normalizes the status label for mails shown inside the inbox context.
   *
   * @returns Status label displayed on the mail list card.
   */
  getStatusLabel(): string {
    if (this.mailbox === 'inbox' && this.mail.status === 'SENT') {
      return 'INBOX';
    }

    return this.mail.status;
  }

  /**
   * Describes whether a mail is an external request, support reply or internal message.
   *
   * @returns Conversation type label shown on the list card.
   */
  getConversationType(): string {
    if (this.mail.deliveryMode === MailDeliveryMode.EXTERNAL && this.mail.sender === null) {
      return 'External incoming';
    }

    if (this.mail.deliveryMode === MailDeliveryMode.EXTERNAL) {
      return 'Support reply';
    }

    return 'Internal mail';
  }

  /**
   * Builds a compact single-line preview from the mail body.
   *
   * @returns Shortened text preview for the list card.
   */
  getPreview(): string {
    const content = this.mail.content?.replace(/\s+/g, ' ').trim();
    if (!content) {
      return 'No text content.';
    }

    return content.length > 110 ? `${content.slice(0, 110).trim()}...` : content;
  }

  /**
   * Formats a timestamp as time for today or a compact date otherwise.
   *
   * @param dateString ISO timestamp returned by the backend.
   * @returns User-facing short date label.
   */
  formatDate(dateString: string): string {
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
   * Opens the selected mail and suppresses default keyboard scrolling for space activation.
   *
   * @param event Optional click or keyboard event from the mail card.
   */
  navigateToMail(event?: Event): void {
    event?.preventDefault();
    this.router.navigate(['/mails', this.mail.id]);
  }
}
