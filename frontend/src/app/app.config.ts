import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import {
  provideRouter,
  withComponentInputBinding,
} from '@angular/router';
import {
  provideHttpClient,
  withFetch,
  withInterceptorsFromDi,
} from '@angular/common/http';
import { provideOAuthClient } from 'angular-oauth2-oidc';
import { providePrimeNG } from 'primeng/config';
import { MessageService } from 'primeng/api';
import Aura from '@primeuix/themes/aura';
import { routes } from './app.routes';
import { AuthService } from '../services/auth/auth-service';

/**
 * The application configuration for the Angular app, including providers for global error handling,
 * routing, HTTP client, OAuth2 authentication, and PrimeNG UI components.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),

    // Angular Router Configuration
    provideRouter(
      routes,
      withComponentInputBinding(),
    ),

    // HTTP Client Configuration
    provideHttpClient(
      withFetch(),
      withInterceptorsFromDi(),
    ),

    // OAuth2 Client Configuration for OpenID Connect (OIDC)
    provideOAuthClient({
      resourceServer: {
        allowedUrls: ['/api'],
        sendAccessToken: true,
      },
    }),

    // Application Initializer for AuthService
    provideAppInitializer(() => {
      return inject(AuthService).initialize();
    }),

    providePrimeNG({
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: false,
        },
      },
    }),
    MessageService,
  ],
};
