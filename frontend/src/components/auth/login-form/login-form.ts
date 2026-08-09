import {
  Component,
  inject,
  OnInit,
} from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth/auth-service';

@Component({
  selector: 'app-login-form',
  templateUrl: './login-form.html',
  styleUrl: './login-form.css',
})
export class LoginForm implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  /**
   * Checks if the user is already authenticated on component initialization and redirects to the mails page if so.
   */
  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      void this.router.navigate(['/mails']);
    }
  }

  /**
   * Redirects the browser to the external identity provider.
   */
  protected login(): void {
    this.authService.login();
  }
}
