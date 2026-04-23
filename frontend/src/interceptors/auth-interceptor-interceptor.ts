import {HttpErrorResponse, HttpInterceptorFn} from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth/auth-service';
import {tap} from 'rxjs';

export const authInterceptorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const authToken = authService.getToken();

  if (!authToken) {
    return next(req);
  }

  const newReq = req.clone({
    headers: req.headers.set('Authorization', `Bearer ${authToken}`),
  });


  return next(newReq).pipe(
    tap({
      error: (error: HttpErrorResponse) => {
        if(error.status === 401) {
          authService.logout();
        }
      }
    }),
  );
};
