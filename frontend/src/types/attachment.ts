import { SafeResourceUrl } from '@angular/platform-browser';

export interface Attachment {
  size: number;
  fileName: string;
  mimeType: string;
  path: string;
  url?: string;
  safeUrl?: SafeResourceUrl;
  blob?: Blob;
}
