import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs';
import { AuthService } from '../services/auth/auth-service';

export const authInterceptorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const authToken = authService.getValidToken();

  if (!authToken) {
    return next(req);
  }

  const newReq = req.clone({
    headers: req.headers.set('Authorization', `Bearer ${authToken}`),
  });

  return next(newReq).pipe(
    tap({
      error: (error: HttpErrorResponse) => {
        if (!isAuthEndpoint(req.url) && isAuthenticationFailure(error)) {
          authService.logout();
        }
      },
    }),
  );
};

function isAuthenticationFailure(error: HttpErrorResponse): boolean {
  const message = error.error?.message;

  return error.status === 401 || message === 'Session expired. Please log in again.';
}

function isAuthEndpoint(url: string): boolean {
  return url.endsWith('/login') || url.endsWith('/register');
}
