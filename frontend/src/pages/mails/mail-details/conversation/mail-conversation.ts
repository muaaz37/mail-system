import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, Input, OnChanges, output, signal, SimpleChanges } from '@angular/core';
import { AvatarModule } from 'primeng/avatar';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MailsService } from '../../../../services/mails/mails-service';
import { Mail, MailDeliveryMode } from '../../../../types/mails';
import { getMailSenderDisplay } from '../../../../utils/mail-senders';

/** Loads and displays the thread of one internal mail conversation. */
@Component({
  selector: 'app-mail-conversation',
  standalone: true,
  imports: [AvatarModule, ProgressSpinnerModule],
  templateUrl: './mail-conversation.html',
  styles: [':host { display: block; }'],
})
export class MailConversation implements OnChanges {
  @Input({ required: true }) currentMail!: Mail;
  @Input() visible = false;

  readonly loadFailed = output<HttpErrorResponse>();

  protected readonly conversation = signal<Mail[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly getMailSenderDisplay = getMailSenderDisplay;

  private readonly mailsService = inject(MailsService);
  private loadedMailId: string | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['currentMail']) {
      this.conversation.set([]);
      this.loadedMailId = null;
    }

    if (this.visible && !this.isLoading() && this.loadedMailId !== this.currentMail.id) {
      this.loadConversation();
    }
  }

  protected formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString();
  }

  protected recipientSummary(mail: Mail): string {
    if (mail.deliveryMode === MailDeliveryMode.EXTERNAL) {
      return mail.externalTo.join(', ') || 'Configured support mailbox';
    }
    return mail.to.map((recipient) => recipient.firstName).join(', ');
  }

  private loadConversation(): void {
    this.isLoading.set(true);
    this.mailsService.getInternalConversation(this.currentMail.id).subscribe({
      next: (conversation) => {
        this.conversation.set(conversation);
        this.loadedMailId = this.currentMail.id;
        this.isLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.loadFailed.emit(error);
      },
    });
  }
}
