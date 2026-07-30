import { Routes } from '@angular/router';
import { LoginPage } from '../pages/login-page/login-page';
import { RootLayout } from '../layouts/root-layout/root-layout';
import { RegisterPage } from '../pages/register-page/register-page';
import { AuthGuard } from '../services/auth/auth-guard';
import { MailSent } from '../pages/mails/mail-sent/mail-sent';
import { MailDrafts } from '../pages/mails/mail-drafts/mail-drafts';
import { MailDetails } from '../pages/mails/mail-details/mail-details';
import { MailCreate } from '../pages/mails/mail-create/mail-create';
import { MailEdit } from '../pages/mails/mail-edit/mail-edit';
import { NoFound } from '../pages/no-found/no-found';
import { TicketsPage } from '../pages/tickets/tickets-page/tickets-page';
import { TicketDetails } from '../pages/tickets/ticket-details/ticket-details';

export const routes: Routes = [
  { path: 'login', component: LoginPage },
  { path: 'register', component: RegisterPage },
  { path: '', redirectTo: '/mails', pathMatch: 'full' },
  {
    path: 'mails',
    component: RootLayout,
    canActivate: [AuthGuard],
    children: [
      { path: '', component: TicketsPage },
      { path: 'waiting', component: TicketsPage },
      { path: 'resolved', component: TicketsPage },
      { path: 'tickets/:id', component: TicketDetails },
      { path: 'sent', component: MailSent },
      { path: 'drafts', component: MailDrafts },
      { path: 'create', component: MailCreate },
      { path: ':id/reply', component: MailCreate },
      { path: ':id', component: MailDetails },
      { path: ':id/edit', component: MailEdit },
    ],
  },
  { path: '404', component: NoFound },
  { path: '**', redirectTo: '/404' },
];
