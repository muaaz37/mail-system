import { Routes, UrlMatchResult, UrlMatcher, UrlSegment } from '@angular/router';
import { LoginPage } from '../pages/login-page/login-page';
import { RootLayout } from '../layouts/root-layout/root-layout';
import { AuthGuard } from '../services/auth/auth-guard';
import { MailSent } from '../pages/mails/mail-sent/mail-sent';
import { MailDrafts } from '../pages/mails/mail-drafts/mail-drafts';
import { MailDetails } from '../pages/mails/mail-details/mail-details';
import { MailCreate } from '../pages/mails/mail-create/mail-create';
import { MailEdit } from '../pages/mails/mail-edit/mail-edit';
import { NoFound } from '../pages/no-found/no-found';
import { TicketsPage } from '../pages/tickets/tickets-page/tickets-page';
import { TicketDetails } from '../pages/tickets/ticket-details/ticket-details';

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function uuidRoute(paramName: string): UrlMatcher {
  return (segments: UrlSegment[]): UrlMatchResult | null => {
    if (segments.length === 1 && uuidPattern.test(segments[0].path)) {
      return { consumed: segments, posParams: { [paramName]: segments[0] } };
    }

    return null;
  };
}

function uuidWithSuffixRoute(paramName: string, suffix: string): UrlMatcher {
  return (segments: UrlSegment[]): UrlMatchResult | null => {
    if (segments.length === 2 && uuidPattern.test(segments[0].path) && segments[1].path === suffix) {
      return { consumed: segments, posParams: { [paramName]: segments[0] } };
    }

    return null;
  };
}

function prefixWithUuidRoute(prefix: string, paramName: string): UrlMatcher {
  return (segments: UrlSegment[]): UrlMatchResult | null => {
    if (segments.length === 2 && segments[0].path === prefix && uuidPattern.test(segments[1].path)) {
      return { consumed: segments, posParams: { [paramName]: segments[1] } };
    }

    return null;
  };
}

export const routes: Routes = [
  { path: 'login', component: LoginPage },
  { path: '', redirectTo: '/mails', pathMatch: 'full' },
  {
    path: 'mails',
    component: RootLayout,
    canActivate: [AuthGuard],
    children: [
      { path: '', component: TicketsPage },
      { path: 'waiting', component: TicketsPage },
      { path: 'resolved', component: TicketsPage },
      { matcher: prefixWithUuidRoute('tickets', 'id'), component: TicketDetails },
      { path: 'sent', component: MailSent },
      { path: 'drafts', component: MailDrafts },
      { path: 'create', component: MailCreate },
      { matcher: uuidWithSuffixRoute('id', 'reply'), component: MailCreate },
      { matcher: uuidWithSuffixRoute('id', 'edit'), component: MailEdit },
      { matcher: uuidRoute('id'), component: MailDetails },
      { path: '**', redirectTo: '/404' },
    ],
  },
  { path: '404', component: NoFound },
  { path: '**', redirectTo: '/404' },
];
