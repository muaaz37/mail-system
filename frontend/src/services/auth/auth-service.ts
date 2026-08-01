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
   * Loads the identity provider metadata and processes a possible login callback.
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
   * Ends the local session and redirects to the identity provider logout.
   */
  public logout(): void {
    this.currentUser = null;
    this.oauthService.logOut();
  }

  /**
   * Returns whether a valid access token is available.
   */
  public isAuthenticated(): boolean {
    return this.oauthService.hasValidAccessToken();
  }

  /**
   * Returns the local application profile of the authenticated identity.
   */
  public getCurrentUser(): User | null {
    return this.currentUser;
  }

  /**
   * initialize the OIDC flow and load the current user
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
   * Loads the current authenticated user's profile from the backend API and stores it in the `currentUser`property.
   */
  private async loadCurrentUser(): Promise<void> {
    this.currentUser = await firstValueFrom(
      this.http.get<User>(`${API_BASE_URL}/users/me`),
    );
  }
}
