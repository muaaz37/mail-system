import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';
import { MailsList } from '../../../components/mails/mails-list/mails-list';
import { MailsService } from '../../../services/mails/mails-service';
import { Mail } from '../../../types/mails';
import { readApiErrorMessage } from '../../../utils/api-error-message';

@Component({
  selector: 'app-mail-inbox',
  imports: [MailsList, Toast],
  templateUrl: './mail-inbox.html',
})
export class MailInbox implements OnInit {
  private mailsService = inject(MailsService);
  private messageService = inject(MessageService);

  protected mails = signal<Mail[]>([]);
  protected isLoading = signal(true);

  /**
   * Loads incoming mails when the inbox page is opened.
   */
  ngOnInit(): void {
    this.loadMails();
  }

  /**
   * Loads all mails visible in the shared inbox view.
   */
  private loadMails(): void {
    this.isLoading.set(true);
    this.mailsService.getIncomingMails().subscribe({
      next: (mails) => {
        this.mails.set(mails);
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
