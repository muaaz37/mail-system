import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, Input, OnChanges } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { MessageService } from 'primeng/api';
import { ImageModule } from 'primeng/image';
import { MailsService } from '../../../../services/mails/mails-service';
import { Attachment } from '../../../../types/attachment';
import { readApiErrorMessage } from '../../../../utils/api-error-message';

/** Displays mail attachments and manages their browser previews. */
@Component({
  selector: 'app-mail-attachment-list',
  standalone: true,
  imports: [ImageModule],
  templateUrl: './mail-attachment-list.html',
  styles: [':host { display: block; }'],
})
export class MailAttachmentList implements OnChanges {
  @Input() attachments: Attachment[] = [];

  private readonly mailsService = inject(MailsService);
  private readonly messageService = inject(MessageService);
  private readonly sanitizer = inject(DomSanitizer);

  ngOnChanges(): void {
    this.attachments
      .filter((attachment) => !attachment.blob)
      .forEach((attachment) => this.loadPreview(attachment));
  }

  protected openAttachment(attachment: Attachment): void {
    if (attachment.url) {
      window.open(attachment.url);
      return;
    }

    this.mailsService.fetchAttachment(attachment.path).subscribe({
      next: (blob) => {
        this.assignBlob(attachment, blob);
        window.open(attachment.url);
      },
      error: (error: HttpErrorResponse) => this.showError('Failed to Open Attachment', error),
    });
  }

  protected isImage(attachment: Attachment): boolean {
    return attachment.mimeType?.startsWith('image/') ?? false;
  }

  protected isPdf(attachment: Attachment): boolean {
    return (
      attachment.mimeType === 'application/pdf' ||
      attachment.fileName?.toLowerCase().endsWith('.pdf')
    );
  }

  private loadPreview(attachment: Attachment): void {
    this.mailsService.fetchAttachment(attachment.path).subscribe({
      next: (blob) => this.assignBlob(attachment, blob),
      error: (error: HttpErrorResponse) => this.showError('Failed to Load Attachment', error),
    });
  }

  private assignBlob(attachment: Attachment, blob: Blob): void {
    const url = URL.createObjectURL(blob);
    attachment.url = url;
    attachment.safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
    attachment.blob = blob;
  }

  private showError(summary: string, error: HttpErrorResponse): void {
    this.messageService.add({
      severity: 'error',
      summary,
      detail: readApiErrorMessage(error),
    });
  }
}
