import {Component, inject, signal} from '@angular/core';
import {MailsService} from '../../../services/mails/mails-service';
import {MessageService} from 'primeng/api';
import {Mail} from '../../../types/mails';
import {MailsList} from '../../../components/mails/mails-list/mails-list';
import {Toast} from 'primeng/toast';

@Component({
  selector: 'app-mail-sent',
  imports: [
    MailsList,
    Toast
  ],
  templateUrl: './mail-sent.html',
  styleUrl: './mail-sent.css',
})
export class MailSent {
  private mailsService = inject(MailsService);
  private messageService = inject(MessageService);

  protected mails = signal<Mail[]>([]);
  protected isLoading = signal(true);

  ngOnInit() {
    this.loadMails();
  }

  private loadMails() {
    this.isLoading.set(true);
    this.mailsService.getSentMails().subscribe({
      next: (mails) => {
        this.mails.set(mails);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Failed to Load Mails',
          detail: err.error?.message || 'An error occurred',
        });
        this.isLoading.set(false);
      },
    });
  }
}
