import {Component, inject, Input, OnInit, signal} from '@angular/core';
import {MailForm} from '../../../components/mails/mail-form/mail-form';
import {MailsService} from '../../../services/mails/mails-service';
import {MessageService} from 'primeng/api';
import {Mail} from '../../../types/mails';

@Component({
  selector: 'app-mail-edit',
  imports: [
    MailForm,
  ],
  templateUrl: './mail-edit.html',
})
export class MailEdit implements OnInit{

  @Input() protected id!: string;

  private mailsService = inject(MailsService);
  private messageService = inject(MessageService);

  protected mail = signal<Mail | null>(null);
  protected isLoading = signal(true);
  protected attachments = signal<string[]>([]);

  ngOnInit() {
    this.loadMail(this.id);
  }

  private loadMail(id: string) {
    this.isLoading.set(true);
    this.mailsService.getMailById(id).subscribe({
      next: (mail) => {
        mail.attachments.forEach(attachment=>{
          this.mailsService.fetchAttachment(attachment.path).subscribe({
            next: (blob) => {
              attachment.url = URL.createObjectURL(blob);
              attachment.blob = blob;
            },
            error: (err) => {
              this.messageService.add({
                severity: 'error',
                summary: 'Failed to Load Attachment',
                detail: err.error?.message || 'An error occurred',
              });
            },
          });
        })
        this.mail.set(mail);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Failed to Load Mail',
          detail: err.error?.message || 'An error occurred',
        });
        this.isLoading.set(false);
      },
    });
  }

}
