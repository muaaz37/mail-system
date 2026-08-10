/**
 * Public backend error shape used for sanitized user-facing error messages.
 */
export interface ErrorResponse {
  status: number;
  message: string;
}
