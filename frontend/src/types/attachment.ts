import { SafeResourceUrl } from '@angular/platform-browser';

/**
 * Attachment metadata returned by the backend and enriched with optional browser preview state.
 */
export interface Attachment {
  size: number;
  fileName: string;
  mimeType: string;
  path: string;
  url?: string;
  safeUrl?: SafeResourceUrl;
  blob?: Blob;
}
