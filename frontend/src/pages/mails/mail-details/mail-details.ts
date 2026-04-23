import {Component, inject, Input, signal} from '@angular/core';
import { Router } from '@angular/router';
import { MailsService } from '../../../services/mails/mails-service';
import { MessageService } from 'primeng/api';
import { Mail } from '../../../types/mails';
import {CommonModule, Location} from '@angular/common';
import { Toast } from 'primeng/toast';
import { TagModule } from 'primeng/tag';
import { AvatarModule } from 'primeng/avatar';
import { DividerModule } from 'primeng/divider';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonModule } from 'primeng/button';
import {getSeverityBadge, getSourceBadge} from '../../../utils/badges';
import { ImageModule } from 'primeng/image';
import {AuthService} from '../../../services/auth/auth-service';

@Component({
  selector: 'app-mail-details',
  standalone: true,
  imports: [
    CommonModule,
    Toast,
    TagModule,
    AvatarModule,
    DividerModule,
    ProgressSpinnerModule,
    TooltipModule,
    ButtonModule,
    ImageModule,
  ],
  templateUrl: './mail-details.html',
  styleUrl: './mail-details.css',
})
export class MailDetails {

  @Input() protected id!: string;

  private authService = inject(AuthService);
  private mailsService = inject(MailsService);
  private messageService = inject(MessageService);
  private router = inject(Router);
  private location = inject(Location);

  protected mail = signal<Mail | null>(null);
  protected isLoading = signal(true);

  ngOnInit() {
    this.loadMail(this.id);
  }

  isUserSender(): boolean {
    if(!this.mail()) return false;
    return this.mail()!.sender.id === this.authService.getCurrentUser()?.id;
  }

  private loadMail(id: string) {
    this.isLoading.set(true);
    this.mailsService.getMailById(id).subscribe({
      next: (mail) => {
        mail.attachments.forEach((attachment) => {
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
        });
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

  goBack() {
    this.location.back()
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString();
  }

  getEmailString(recipients: any[] | undefined): string {
    if (!recipients) return '';
    return recipients.map((r) => `${r.firstName} ${r.lastName} (${r.email})`).join(', ');
  }

  editMail() {
    const mail = this.mail();
    if (mail) {
      this.router.navigate(['/mails', mail.id, 'edit']);
    }
  }

  sendMail() {
    const mail = this.mail();
    if (mail) {
      this.mailsService.sendMail(mail.id).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Mail Sent',
            detail: 'The mail has been sent successfully',
          });
          this.router.navigate(['/mails/sent']);
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Failed to Send Mail',
            detail: err.error?.message || 'An error occurred',
          });
        },
      });
    }
  }

  deleteMail() {
    const mail = this.mail();
    if (mail) {
      this.mailsService.deleteMail(mail.id).subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Mail Deleted',
            detail: 'The mail has been deleted successfully',
          });
          this.router.navigate(['/mails/drafts']);
        },
        error: (err) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Failed to Delete Mail',
            detail: err.error?.message || 'An error occurred',
          });
        },
      });
    }
  }

  protected readonly getSeverityBadge = getSeverityBadge;
  protected readonly getSourceBadge = getSourceBadge;
}
