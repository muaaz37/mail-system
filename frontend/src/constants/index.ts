/**
 * Relative backend API base path used behind the Caddy reverse proxy.
 */
export const API_BASE_URL = '/api';

interface RuntimeConfig {
  mailboxRefreshIntervalMs?: unknown;
  mailboxRefreshTimeoutMs?: unknown;
}

declare global {
  interface Window {
    mailSystemConfig?: RuntimeConfig;
  }
}

const DEFAULT_MAILBOX_REFRESH_INTERVAL_MS = 15000;
const DEFAULT_MAILBOX_REFRESH_TIMEOUT_MS = 10000;
const MIN_MAILBOX_REFRESH_INTERVAL_MS = 5000;
const MAX_MAILBOX_REFRESH_INTERVAL_MS = 120000;
const MIN_MAILBOX_REFRESH_TIMEOUT_MS = 3000;
const MAX_MAILBOX_REFRESH_TIMEOUT_MS = 30000;

/**
 * Reads a positive millisecond value from runtime configuration and clamps unsafe values.
 */
function readDuration(
  value: unknown,
  fallback: number,
  minimum: number,
  maximum: number,
): number {
  const parsed = Number(value);

  if (!Number.isFinite(parsed)) {
    return fallback;
  }

  return Math.min(Math.max(Math.trunc(parsed), minimum), maximum);
}

/**
 * Runtime-configured interval used by mailbox views to reflect server-side IMAP imports.
 */
export const MAILBOX_REFRESH_INTERVAL_MS = readDuration(
  window.mailSystemConfig?.mailboxRefreshIntervalMs,
  DEFAULT_MAILBOX_REFRESH_INTERVAL_MS,
  MIN_MAILBOX_REFRESH_INTERVAL_MS,
  MAX_MAILBOX_REFRESH_INTERVAL_MS,
);

/**
 * Maximum time a background mailbox refresh request may occupy before the next cycle can continue.
 */
export const MAILBOX_REFRESH_TIMEOUT_MS = readDuration(
  window.mailSystemConfig?.mailboxRefreshTimeoutMs,
  DEFAULT_MAILBOX_REFRESH_TIMEOUT_MS,
  MIN_MAILBOX_REFRESH_TIMEOUT_MS,
  Math.min(MAX_MAILBOX_REFRESH_TIMEOUT_MS, MAILBOX_REFRESH_INTERVAL_MS),
);

/**
 * The URL of the OpenID Connect (OIDC) issuer, which is the Keycloak server in this case.
 */
export const OIDC_ISSUER_URL =
  `${window.location.origin}/realms/mail-support`;

/**
 * The client ID for the OpenID Connect (OIDC) authentication,
 * which is used to identify the application to the Keycloak server.
 */
export const OIDC_CLIENT_ID = 'mail-client';
