import { inject, Injectable } from '@angular/core';

import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../../constants';
import { AuthResponse, LoginRequest, RegisterRequest } from '../../types/auth';
import { Observable } from 'rxjs';
import { ErrorResponse } from '../../types/error';
import { Router } from '@angular/router';

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
    return this.http.post<AuthResponse | ErrorResponse>(
      `${API_BASE_URL}/register`,
      credentials,
    );

  }

  public logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.router.navigate(['login']);
  }

  public isAuthenticated(): boolean {
    return !!localStorage.getItem('token');
  }

  public getToken(): string | null {
    return localStorage.getItem('token');
  }

  public getCurrentUser() {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }
}
