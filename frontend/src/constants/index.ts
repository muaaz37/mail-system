/**
 * Relative backend API base path used behind the Caddy reverse proxy.
 */
export const API_BASE_URL = '/api';

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
