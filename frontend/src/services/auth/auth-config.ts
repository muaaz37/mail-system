import { AuthConfig } from 'angular-oauth2-oidc';
import { OIDC_CLIENT_ID, OIDC_ISSUER_URL } from '../../constants';

/**
 * The document base URI is `/app/` in Docker and `/` with the Angular dev server.
 */
const applicationBaseUrl = new URL(document.baseURI).toString();

/**
 * OpenID Connect configuration for the external identity provider.
 */
export const authConfig: AuthConfig = {
  issuer: OIDC_ISSUER_URL,
  clientId: OIDC_CLIENT_ID,
  responseType: 'code', // activate PKCE (Proof Key for Code Exchange)
  redirectUri: applicationBaseUrl,
  postLogoutRedirectUri: applicationBaseUrl,
  scope: 'openid profile email',
  requireHttps: location.hostname !== 'localhost',
  strictDiscoveryDocumentValidation: true,
  showDebugInformation: false,
  sessionChecksEnabled: true,
  useSilentRefresh: false,
};

