import { HttpErrorResponse } from '@angular/common/http';

const SERVER_UNAVAILABLE_MESSAGE = 'Server is currently unavailable. Please try again after it starts.';
const SESSION_EXPIRED_MESSAGE = 'Session expired. Please log in again.';
const PERMISSION_DENIED_MESSAGE = 'You do not have permission to perform this action.';
const ATTACHMENT_TOO_LARGE_MESSAGE = 'Attachment is too large. Maximum file size is 10 MB.';
const UNEXPECTED_ERROR_MESSAGE = 'Unexpected server error. Please try again later.';

const SAFE_SERVER_MESSAGES = new Set([
  'Mail could not be sent. The draft was kept for retry.',
  'Attachment storage is currently unavailable.',
  'Attachment was not found.',
  UNEXPECTED_ERROR_MESSAGE,
]);

const TECHNICAL_MESSAGE_PATTERNS = [
  /exception/i,
  /stack trace/i,
  /\bjava\./i,
  /\borg\.springframework\./i,
  /\bSQLException\b/i,
  /\bSQLState\b/i,
  /connection refused/i,
  /localhost:\d+/i,
  /https?:\/\//i,
];

export function readApiErrorMessage(error: unknown, fallback = 'An error occurred'): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  if (error.status === 0) {
    return SERVER_UNAVAILABLE_MESSAGE;
  }

  const apiMessage = extractApiMessage(error);

  if (error.status === 401) {
    return apiMessage === 'Invalid credentials' ? 'Invalid email or password.' : SESSION_EXPIRED_MESSAGE;
  }

  if (error.status === 403) {
    return PERMISSION_DENIED_MESSAGE;
  }

  if (error.status === 413) {
    return apiMessage && !looksTechnical(apiMessage) ? apiMessage : ATTACHMENT_TOO_LARGE_MESSAGE;
  }

  if (error.status >= 500) {
    return apiMessage && SAFE_SERVER_MESSAGES.has(apiMessage) ? apiMessage : UNEXPECTED_ERROR_MESSAGE;
  }

  if (apiMessage && !looksTechnical(apiMessage)) {
    return apiMessage;
  }

  return fallback;
}

function extractApiMessage(error: HttpErrorResponse): string | null {
  const message = error.error?.message;
  return typeof message === 'string' && message.trim().length > 0 ? message.trim() : null;
}

function looksTechnical(message: string): boolean {
  return TECHNICAL_MESSAGE_PATTERNS.some((pattern) => pattern.test(message));
}
