import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { OAuthService } from 'angular-oauth2-oidc';

import { AuthService } from './auth-service';

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        { provide: OAuthService, useValue: {} },
      ],
    });
    service = TestBed.inject(AuthService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
