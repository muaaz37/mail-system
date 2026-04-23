import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../../services/auth/auth-service';
import { FloatLabelModule } from 'primeng/floatlabel';
import {Router, RouterLink} from '@angular/router';

@Component({
  selector: 'app-login-form',
  imports: [
    MessageModule,
    ToastModule,
    ButtonModule,
    InputTextModule,
    ReactiveFormsModule,
    FloatLabelModule,
    RouterLink,
  ],
  providers: [MessageService],
  templateUrl: './login-form.html',
})
export class LoginForm {
  protected loginForm = new FormGroup({
    email: new FormControl('', [Validators.email, Validators.required]),
    password: new FormControl('', [Validators.minLength(6), Validators.required]),
  });

  private authService = inject(AuthService);
  private messageService = inject(MessageService);
  private router = inject(Router);

  protected formSubmitted = signal(false);

  async onSubmit() {
    if (this.loginForm.valid) {
      this.authService
        .login({
          email: this.loginForm.value.email || '',
          password: this.loginForm.value.password || '',
        })
        .subscribe({
          next: (res) => {
            if ('token' in res) {
              localStorage.setItem('token', res.token);
              localStorage.setItem('user', JSON.stringify(res.user));
              this.router.navigate(['mails']);
            }
          },
          error: (err) => {
            this.messageService.add({
              severity: 'error',
              summary: 'Login Failed',
              detail: err.error.message,
            });
          },
        });
    }
  }

  isInvalid(controlName: string) {
    const control = this.loginForm.get(controlName);
    return control?.invalid && (control.touched || this.formSubmitted());
  }
}
