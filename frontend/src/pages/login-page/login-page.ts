import { Component } from '@angular/core';
import { LoginForm } from '../../components/auth/login-form/login-form';

/**
 * Public entry page that keeps application branding visible before redirecting to Keycloak login.
 */
@Component({
  selector: 'app-login-page',
  imports: [LoginForm],
  templateUrl: './login-page.html',
})
export class LoginPage {}
