import { Component, Input, inject } from '@angular/core';
import { Router} from '@angular/router';
import { Mail } from '../../../types/mails';
import { TagModule } from 'primeng/tag';
import {getSeverityBadge} from '../../../utils/badges';

@Component({
  selector: 'app-mails-list-element',
  imports: [TagModule],
  templateUrl: './mails-list-element.html',
})
export class MailsListElement {
  @Input() mail!: Mail;

  private router = inject(Router);
  protected readonly getSeverityBadge = getSeverityBadge;

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

  navigateToMail() {
    this.router.navigate(['/mails', this.mail.id]);
  }


}
