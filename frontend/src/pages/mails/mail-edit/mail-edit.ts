import { CommonModule } from '@angular/common';
import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { MessageService } from 'primeng/api';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { Toast } from 'primeng/toast';
import { forkJoin, map } from 'rxjs';
import { MailForm } from '../../../components/mails/mail-form/mail-form';
import { MailsService } from '../../../services/mails/mails-service';
import { Mail } from '../../../types/mails';
import { readApiErrorMessage } from '../../../utils/api-error-message';

@Component({
  selector: 'app-mail-edit',
  imports: [CommonModule, MailForm, ProgressSpinnerModule, Toast],
  templateUrl: './mail-edit.html',
})
export class MailEdit implements OnInit {
  @Input() protected id!: string;

  private mailsService = inject(MailsService);
  private messageService = inject(MessageService);

  protected mail = signal<Mail | null>(null);
  protected isLoading = signal(true);

  /**
   * Loads the draft selected by the edit route.
   */
  ngOnInit(): void {
    this.loadMail(this.id);
  }

  /**
   * Loads a draft and preloads attachment blobs so retained attachments can be resubmitted.
   *
   * @param id Draft mail identifier from the route.
   */
  private loadMail(id: string): void {
    this.isLoading.set(true);
    this.mailsService.getMailById(id).subscribe({
      next: (mail) => {
        if (!mail.attachments.length) {
          this.mail.set(mail);
          this.isLoading.set(false);
          return;
        }

        const attachmentLoads = mail.attachments.map((attachment) =>
          this.mailsService.fetchAttachment(attachment.path).pipe(
            map((blob) => {
              // Draft updates replace the attachment set, so existing attachments need their blobs loaded.
              attachment.url = URL.createObjectURL(blob);
              attachment.blob = blob;
              return attachment;
            }),
          ),
        );

        forkJoin(attachmentLoads).subscribe({
          next: () => {
            this.mail.set(mail);
            this.isLoading.set(false);
          },
          error: (err) => {
            this.messageService.add({
              severity: 'error',
              summary: 'Failed to Load Attachment',
              detail: readApiErrorMessage(err),
            });
            this.isLoading.set(false);
          },
        });
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Failed to Load Mail',
          detail: readApiErrorMessage(err),
        });
        this.isLoading.set(false);
      },
    });
  }
}
