import { Routes } from '@angular/router';
import { LoginPage } from '../pages/login-page/login-page';
import { RootLayout } from '../layouts/root-layout/root-layout';
import { RegisterPage } from '../pages/register-page/register-page';
import { AuthGuard } from '../services/auth/auth-guard';
import {MailSent} from '../pages/mails/mail-sent/mail-sent';
import {MailDrafts} from '../pages/mails/mail-drafts/mail-drafts';
import {MailInbox} from '../pages/mails/mail-inbox/mail-inbox';
import { MailDetails } from '../pages/mails/mail-details/mail-details';
import {MailCreate} from '../pages/mails/mail-create/mail-create';
import {MailEdit} from '../pages/mails/mail-edit/mail-edit';
import { NoFound } from '../pages/no-found/no-found';

export const routes: Routes = [
  { path: 'login', component: LoginPage },
  { path: 'register', component: RegisterPage },
  { path: '', redirectTo: '/mails', pathMatch: 'full' },
  { path: 'mails', component: RootLayout, canActivate: [AuthGuard],
     children: [
       {path: '', component: MailInbox},
       {path: 'sent', component: MailSent},
       {path: 'drafts', component: MailDrafts},
       {path: 'create', component: MailCreate},
       {path: ':id', component: MailDetails},
       {path: ':id/edit', component: MailEdit}
    ]
  },
  { path: '404', component: NoFound },
  { path: '**', redirectTo: '/404' },
];
