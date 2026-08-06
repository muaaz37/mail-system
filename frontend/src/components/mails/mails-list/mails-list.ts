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
  @Input() mails: Mail[] = [];
  @Input() isLoading = false;
  @Input() title = '';
  @Input() mailbox: 'inbox' | 'sent' | 'drafts' = 'inbox';

  /**
   * Describes the currently displayed mailbox for the list header.
   *
   * @returns Context-specific mailbox description.
   */
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

  /**
   * Labels the primary list metric for the current mailbox.
   *
   * @returns Metric label displayed near the list count.
   */
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

  /**
   * Names the active mailbox view for compact UI context.
   *
   * @returns Short mailbox view label.
   */
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

  /**
   * Counts mails that include at least one attachment.
   *
   * @returns Number of mails with attachments.
   */
  get attachmentCount(): number {
    return this.mails.filter((mail) => mail.attachments.length > 0).length;
  }

  /**
   * Counts mails that belong to the external support-mail workflow.
   *
   * @returns Number of external mails in the current list.
   */
  get externalCount(): number {
    return this.mails.filter((mail) => mail.deliveryMode === 'EXTERNAL').length;
  }

  /**
   * Counts mails that should draw support-team attention.
   *
   * @returns Number of received or failed mails in the current list.
   */
  get needsAttentionCount(): number {
    return this.mails.filter((mail) => mail.status === MailStatus.RECEIVED || mail.status === MailStatus.ERROR).length;
  }
}
