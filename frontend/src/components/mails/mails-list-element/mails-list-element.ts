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

  private router = inject(Router);
  protected readonly getSeverityBadge = getSeverityBadge;
  protected readonly getMailSenderDisplay = getMailSenderDisplay;

  getStatusLabel(): string {
    if (this.mailbox === 'inbox' && this.mail.status === 'SENT') {
      return 'INBOX';
    }

    return this.mail.status;
  }

  getConversationType(): string {
    if (this.mail.deliveryMode === MailDeliveryMode.EXTERNAL && this.mail.sender === null) {
      return 'External incoming';
    }

    if (this.mail.deliveryMode === MailDeliveryMode.EXTERNAL) {
      return 'Support reply';
    }

    return 'Internal mail';
  }

  getPreview(): string {
    const content = this.mail.content?.replace(/\s+/g, ' ').trim();
    if (!content) {
      return 'No text content.';
    }

    return content.length > 110 ? `${content.slice(0, 110).trim()}...` : content;
  }

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

  navigateToMail(event?: Event) {
    event?.preventDefault();
    this.router.navigate(['/mails', this.mail.id]);
  }
}
