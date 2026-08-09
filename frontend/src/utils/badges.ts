/**
 * PrimeNG tag severity values used by badge helper functions.
 */
export type BadgeSeverity = 'success' | 'secondary' | 'info' | 'warn' | 'danger' | 'contrast';

/**
 * Maps a mail status to the PrimeNG severity used by status tags.
 *
 * @param status Backend mail status.
 * @returns PrimeNG severity name.
 */
export const getSeverityBadge = (status: string): BadgeSeverity => {
  switch (status) {
    case 'SENT':
      return 'success';
    case 'DRAFT':
      return 'info';
    case 'ERROR':
      return 'danger';
    default:
      return 'info';
  }
};

/**
 * Maps the mail source to the PrimeNG severity used by source tags.
 *
 * @param source Backend mail source.
 * @returns PrimeNG severity name.
 */
export const getSourceBadge = (source: string): BadgeSeverity => {
  switch (source) {
    case 'INTERN':
      return 'secondary';
    case 'EXTERN':
      return 'warn';
    default:
      return 'info';
  }
};
