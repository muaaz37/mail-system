import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../constants';
import { AuthResponse, LoginRequest, RegisterRequest } from '../../types/auth';
import { ErrorResponse } from '../../types/error';

interface JwtPayload {
  exp?: number;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  public login(credentials: LoginRequest): Observable<AuthResponse | ErrorResponse> {
    return this.http.post<AuthResponse | ErrorResponse>(`${API_BASE_URL}/login`, credentials);
  }

  public register(credentials: RegisterRequest): Observable<AuthResponse | ErrorResponse> {
    return this.http.post<AuthResponse | ErrorResponse>(`${API_BASE_URL}/register`, credentials);
  }

  public storeSession(authResponse: AuthResponse): void {
    localStorage.setItem('token', authResponse.token);
    localStorage.setItem('user', JSON.stringify(authResponse.user));
  }

  public logout(): void {
    this.clearSession();
    this.router.navigate(['login']);
  }

  public clearSession(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  public isAuthenticated(): boolean {
    return this.getValidToken() !== null;
  }

  public getValidToken(): string | null {
    const token = this.getToken();
    if (!token || this.isTokenExpired(token)) {
      this.clearSession();
      return null;
    }

    return token;
  }

  public getToken(): string | null {
    return localStorage.getItem('token');
  }

  public getCurrentUser() {
    const user = localStorage.getItem('user');
    if (!user) {
      return null;
    }

    try {
      return JSON.parse(user);
    } catch {
      this.clearSession();
      return null;
    }
  }

  private isTokenExpired(token: string): boolean {
    const payload = this.decodeJwtPayload(token);
    if (!payload?.exp) {
      return true;
    }

    return payload.exp * 1000 <= Date.now();
  }

  private decodeJwtPayload(token: string): JwtPayload | null {
    try {
      const payload = token.split('.')[1];
      if (!payload) {
        return null;
      }

      const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/');
      const paddedPayload = normalizedPayload.padEnd(
        Math.ceil(normalizedPayload.length / 4) * 4,
        '=',
      );

      return JSON.parse(window.atob(paddedPayload)) as JwtPayload;
    } catch {
      return null;
    }
  }
}
