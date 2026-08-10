import { Component, input, model } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { FileRemoveEvent, FileSelectEvent, FileUploadModule } from 'primeng/fileupload';
import { ImageModule } from 'primeng/image';
import { Attachment } from '../../../../types/attachment';

/** Prepares new and retained files for the backend replacement endpoint. */
export function buildAttachmentFiles(
  uploadedFiles: readonly File[],
  retainedAttachments: readonly Attachment[],
): File[] {
  const retainedFiles = retainedAttachments
    .filter((attachment): attachment is Attachment & { blob: Blob } => !!attachment.blob)
    .map(
      (attachment) =>
        new File([attachment.blob], attachment.fileName, { type: attachment.blob.type }),
    );

  return [...uploadedFiles, ...retainedFiles];
}

@Component({
  selector: 'app-mail-form-attachments',
  standalone: true,
  imports: [ButtonModule, FileUploadModule, ImageModule],
  templateUrl: './mail-form-attachments.html',
})
export class MailFormAttachments {
  readonly attachments = model.required<Attachment[]>();
  readonly uploadedFiles = model.required<File[]>();
  readonly acceptedTypes = input.required<string>();
  readonly maximumSizeBytes = input.required<number>();
  readonly maximumSizeLabel = input.required<string>();

  /** Handle file selection event and update the uploaded files list. */
  protected onFileSelect(event: FileSelectEvent): void {
    this.uploadedFiles.update((files) => [...files, ...event.files]);
  }

  /** Remove a file from the uploaded files list. */
  protected onFileRemove(event: FileRemoveEvent): void {
    this.uploadedFiles.update((files) => files.filter((file) => file !== event.file));
  }

  /** Remove an existing attachment from the attachments list. */
  protected removeExistingFile(attachment: Attachment): void {
    this.attachments.update((attachments) =>
      attachments.filter((candidate) => candidate.url !== attachment.url),
    );
  }

  /** Check if the attachment is an image attachment. */
  protected isImageAttachment(attachment: Attachment): boolean {
    return attachment.mimeType?.startsWith('image/') ?? false;
  }

  /** Check if the attachment is a PDF attachment. */
  protected isPdfAttachment(attachment: Attachment): boolean {
    return (
      attachment.mimeType === 'application/pdf' ||
      attachment.fileName.toLowerCase().endsWith('.pdf')
    );
  }
}
