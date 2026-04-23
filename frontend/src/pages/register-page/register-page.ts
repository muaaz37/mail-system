import { Component } from '@angular/core';
import { RegisterForm } from '../../components/auth/register-form/register-form';

@Component({
  selector: 'app-register-page',
  imports: [RegisterForm],
  templateUrl: './register-page.html',
  styleUrl: './register-page.css',
})
export class RegisterPage {}
