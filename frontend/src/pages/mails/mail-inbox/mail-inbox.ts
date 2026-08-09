import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { Toast } from 'primeng/toast';
import { MailsService } from '../../../services/mails/mails-service';
import { Mail, MailDeliveryMode } from '../../../types/mails';
import { readApiErrorMessage } from '../../../utils/api-error-message';
import { getMailSenderDisplay } from '../../../utils/mail-senders';

@Component({
  selector: 'app-mail-inbox',
  imports: [ProgressSpinnerModule, TagModule, Toast],
  templateUrl: './mail-inbox.html',
  styleUrl: './mail-inbox.css',
})
export class MailInbox implements OnInit {
  private readonly mailsService = inject(MailsService);
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);

  protected mails = signal<Mail[]>([]);
  protected isLoading = signal(true);

  /**
   * Loads incoming mails when the inbox page is opened.
   */
  ngOnInit(): void {
    this.loadMails();
  }

  /** Opens the selected internal message. */
  protected openMail(mail: Mail): void {
    this.router.navigate(['/mails', mail.id]);
  }

  /** Formats a timestamp like the external ticket overview. */
  protected formatDate(dateString: string): string {
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

  /** Builds a compact preview without changing the original message content. */
  protected preview(mail: Mail): string {
    const content = mail.content?.replace(/\s+/g, ' ').trim();
    if (!content) return 'No text content.';
    return content.length > 120 ? `${content.slice(0, 120).trim()}...` : content;
  }

  protected readonly getMailSenderDisplay = getMailSenderDisplay;

  /**
   * Loads internal mails addressed to the authenticated user.
   */
  private loadMails(): void {
    this.isLoading.set(true);
    this.mailsService.getIncomingMails().subscribe({
      next: (mails) => {
        const internalMails = mails
          .filter((mail) => mail.deliveryMode === MailDeliveryMode.INTERNAL)
          .sort(
            (first, second) =>
              new Date(second.sentAt || second.createdAt).getTime() -
              new Date(first.sentAt || first.createdAt).getTime(),
          );

        this.mails.set(internalMails);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Failed to Load Mails',
          detail: readApiErrorMessage(err),
        });
        this.isLoading.set(false);
      },
    });
  }
}
