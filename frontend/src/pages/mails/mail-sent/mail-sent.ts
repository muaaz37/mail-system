import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';
import { MailsList } from '../../../components/mails/mails-list/mails-list';
import { MailsService } from '../../../services/mails/mails-service';
import { Mail } from '../../../types/mails';
import { readApiErrorMessage } from '../../../utils/api-error-message';

@Component({
  selector: 'app-mail-sent',
  imports: [MailsList, Toast],
  templateUrl: './mail-sent.html',
  styleUrl: './mail-sent.css',
})
export class MailSent implements OnInit {
  private readonly mailsService = inject(MailsService);
  private readonly messageService = inject(MessageService);

  protected mails = signal<Mail[]>([]);
  protected isLoading = signal(true);

  /**
   * Loads sent mails when the sent mailbox page is opened.
   */
  ngOnInit(): void {
    this.loadMails();
  }

  /**
   * Loads mails sent by the authenticated support user.
   */
  private loadMails(): void {
    this.isLoading.set(true);
    this.mailsService.getSentMails().subscribe({
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
