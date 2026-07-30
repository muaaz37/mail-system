import {Component, inject, OnInit, signal} from '@angular/core';
import {MailsList} from '../../../components/mails/mails-list/mails-list';
import {MailsService} from '../../../services/mails/mails-service';
import {MessageService} from 'primeng/api';
import {Mail} from '../../../types/mails';
import {Toast} from 'primeng/toast';
import { readApiErrorMessage } from '../../../utils/api-error-message';


@Component({
  selector: 'app-mail-inbox',
  imports: [
    MailsList,
    Toast
  ],
  templateUrl: './mail-inbox.html',
})
export class MailInbox implements OnInit {
  private mailsService = inject(MailsService);
  private messageService = inject(MessageService);

  protected mails = signal<Mail[]>([]);
  protected isLoading = signal(true);

  ngOnInit() {
    this.loadMails();
  }

  private loadMails() {
    this.isLoading.set(true);
    this.mailsService.getIncomingMails().subscribe({
      next: (mails) => {
        this.mails.set(mails);
        this.isLoading.set(false);
      },
      error: (err) => {
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
