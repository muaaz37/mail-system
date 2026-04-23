import { inject, Injectable } from '@angular/core';
import { AuthService } from './auth-service';
import { CanActivate } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  private authService = inject(AuthService);

  public canActivate(): boolean {
    if (this.authService.isAuthenticated()) {
      return true;
    } else {
      this.authService.logout();
      return false;
    }
  }
}
