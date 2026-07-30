import { Component, Input } from '@angular/core';
import { Mail, MailStatus } from '../../../types/mails';
import { MailsListElement } from '../mails-list-element/mails-list-element';

@Component({
  selector: 'app-mails-list',
  imports: [MailsListElement],
  providers: [],
  templateUrl: './mails-list.html',
  styleUrl: './mails-list.css',
})
export class MailsList {

  @Input () mails: Mail[] = [];
  @Input() isLoading = false;
  @Input() title = '';
  @Input() mailbox: 'inbox' | 'sent' | 'drafts' = 'inbox';

  get description(): string {
    switch (this.mailbox) {
      case 'sent':
        return 'Messages that were sent through the application.';
      case 'drafts':
        return 'Saved drafts that can still be edited or sent.';
      default:
        return 'Received messages and support-related activity.';
    }
  }

  get primaryMetricLabel(): string {
    switch (this.mailbox) {
      case 'sent':
        return 'sent mails';
      case 'drafts':
        return 'drafts open';
      default:
        return 'mails';
    }
  }

  get viewContext(): string {
    switch (this.mailbox) {
      case 'sent':
        return 'Sent view';
      case 'drafts':
        return 'Draft view';
      default:
        return 'Inbox view';
    }
  }

  get attachmentCount(): number {
    return this.mails.filter((mail) => mail.attachments.length > 0).length;
  }

  get externalCount(): number {
    return this.mails.filter((mail) => mail.deliveryMode === 'EXTERNAL').length;
  }

  get needsAttentionCount(): number {
    return this.mails.filter((mail) => mail.status === MailStatus.RECEIVED || mail.status === MailStatus.ERROR).length;
  }

}
