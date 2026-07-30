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
  selector: 'app-register-form',
  imports: [
    MessageModule,
    ToastModule,
    InputTextModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  providers: [MessageService],
  templateUrl: './register-form.html',
  styleUrl: './register-form.css',
})
export class RegisterForm implements OnInit {
  protected registerForm = new FormGroup({
    firstName: new FormControl('', [Validators.required]),
    lastName: new FormControl('', [Validators.required]),
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

    if (this.registerForm.valid) {
      this.authService
        .register({
          firstName: this.registerForm.value.firstName || '',
          lastName: this.registerForm.value.lastName || '',
          email: this.registerForm.value.email || '',
          password: this.registerForm.value.password || '',
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
              summary: 'Registration Failed',
              detail: readApiErrorMessage(err, 'Registration failed.'),
            });
          },
        });
    }
  }

  isInvalid(controlName: string) {
    const control = this.registerForm.get(controlName);
    return control?.invalid && (control.touched || this.formSubmitted());
  }
}
