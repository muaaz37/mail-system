import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../../services/auth/auth-service';
import { Router, RouterLink } from '@angular/router';
import { readApiErrorMessage } from '../../../utils/api-error-message';

@Component({
  selector: 'app-login-form',
  imports: [
    MessageModule,
    ToastModule,
    InputTextModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  providers: [MessageService],
  templateUrl: './login-form.html',
  styleUrl: './login-form.css',
})
export class LoginForm implements OnInit {
  protected loginForm = new FormGroup({
    email: new FormControl('', [Validators.email, Validators.required]),
    password: new FormControl('', [Validators.minLength(6), Validators.required]),
  });

  private authService = inject(AuthService);
  private messageService = inject(MessageService);
  private router = inject(Router);

  protected formSubmitted = signal(false);

  ngOnInit() {
    this.authService.clearSession();
  }

  onSubmit() {
    this.formSubmitted.set(true);

    if (this.loginForm.valid) {
      this.authService
        .login({
          email: this.loginForm.value.email || '',
          password: this.loginForm.value.password || '',
        })
        .subscribe({
          next: (res) => {
            if ('token' in res) {
              this.authService.storeSession(res);
              this.router.navigate(['mails']);
            }
          },
          error: (err) => {
            this.messageService.add({
              severity: 'error',
              summary: 'Login Failed',
              detail: readApiErrorMessage(err, 'Login failed.'),
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
