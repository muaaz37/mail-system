import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MessageService } from 'primeng/api';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { Toast } from 'primeng/toast';
import { MailForm } from '../../../components/mails/mail-form/mail-form';
import { MailsService } from '../../../services/mails/mails-service';
import { MailReplyTemplate } from '../../../types/mails';
import { readApiErrorMessage } from '../../../utils/api-error-message';

@Component({
  selector: 'app-mail-create',
  imports: [MailForm, ProgressSpinnerModule, Toast],
  templateUrl: './mail-create.html',
})
export class MailCreate implements OnInit {
  @Input() protected id?: string;

  private mailsService = inject(MailsService);
  private messageService = inject(MessageService);

  protected replyTemplate = signal<MailReplyTemplate | null>(null);
  protected isLoading = signal(false);

  ngOnInit() {
    if (this.id) {
      this.loadReplyTemplate(this.id);
    }
  }

  protected title(): string {
    return this.id ? 'Reply to support mail' : 'Create mail';
  }

  private loadReplyTemplate(id: string) {
    this.isLoading.set(true);
    this.mailsService.getReplyTemplate(id).subscribe({
      next: (template) => {
        this.replyTemplate.set(template);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Failed to Prepare Reply',
          detail: readApiErrorMessage(err),
        });
        this.isLoading.set(false);
      },
    });
  }
}
