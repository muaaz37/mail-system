import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from '../../constants';
import { User } from '../../types/user';
import { authConfig } from './auth-config';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly oauthService = inject(OAuthService);
  private readonly http = inject(HttpClient);

  private initializationPromise?: Promise<void>;
  private currentUser: User | null = null;

  /**
   * Loads identity provider metadata and processes a possible OIDC login callback once.
   *
   * @returns A shared promise that resolves when authentication bootstrap is finished.
   */
  public initialize(): Promise<void> {
    if (!this.initializationPromise) {
      this.initializationPromise = this.initializeOidc();
    }

    return this.initializationPromise;
  }

  /**
   * Starts the OpenID Connect Authorization Code Flow with PKCE.
   */
  public login(): void {
    this.oauthService.initCodeFlow();
  }

  /**
   * Clears the cached profile and redirects to the identity provider logout endpoint.
   */
  public logout(): void {
    this.currentUser = null;
    this.oauthService.logOut();
  }

  /**
   * Checks whether the browser currently holds a valid access token.
   *
   * @returns True when an unexpired access token is available.
   */
  public isAuthenticated(): boolean {
    return this.oauthService.hasValidAccessToken();
  }

  /**
   * Returns the local application profile linked to the authenticated OIDC identity.
   *
   * @returns The current profile, or null when no valid session is available.
   */
  public getCurrentUser(): User | null {
    return this.currentUser;
  }

  /**
   * Configures the OAuth client, handles login redirects and loads the backend user profile.
   *
   * @returns A promise that resolves after OIDC initialization completes.
   */
  private async initializeOidc(): Promise<void> {
    this.oauthService.configure(authConfig);

    try {
      await this.oauthService.loadDiscoveryDocumentAndTryLogin();

      if (!this.oauthService.hasValidAccessToken()) {
        this.currentUser = null;
        return;
      }

      await this.loadCurrentUser();
      this.oauthService.setupAutomaticSilentRefresh();
    } catch (error) {
      // A stale or invalid session must not prevent the application from rendering.
      this.currentUser = null;
      this.oauthService.logOut(true);
      console.error('OpenID Connect initialization failed.', error);
    }
  }

  /**
   * Loads the authenticated user's backend profile and caches it for UI decisions.
   *
   * @returns A promise that resolves after the profile is loaded.
   */
  private async loadCurrentUser(): Promise<void> {
    this.currentUser = await firstValueFrom(
      this.http.get<User>(`${API_BASE_URL}/users/me`),
    );
  }
}
