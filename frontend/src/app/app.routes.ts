import { Routes, UrlMatchResult, UrlMatcher, UrlSegment } from '@angular/router';
import { LoginPage } from '../pages/login-page/login-page';
import { RootLayout } from '../layouts/root-layout/root-layout';
import { AuthGuard } from '../services/auth/auth-guard';
import { MailSent } from '../pages/mails/mail-sent/mail-sent';
import { MailInbox } from '../pages/mails/mail-inbox/mail-inbox';
import { MailDrafts } from '../pages/mails/mail-drafts/mail-drafts';
import { MailDetails } from '../pages/mails/mail-details/mail-details';
import { MailCreate } from '../pages/mails/mail-create/mail-create';
import { MailEdit } from '../pages/mails/mail-edit/mail-edit';
import { NoFound } from '../pages/no-found/no-found';
import { TicketsPage } from '../pages/tickets/tickets-page/tickets-page';
import { TicketDetails } from '../pages/tickets/ticket-details/ticket-details';

/**
 * Matches UUID route segments used by mail and ticket detail routes.
 */
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

/**
 * Creates a matcher for routes that consist only of one UUID segment.
 *
 * @param paramName Name under which the UUID segment is exposed to the component.
 * @returns Angular route matcher for a single UUID path.
 */
function uuidRoute(paramName: string): UrlMatcher {
  return (segments: UrlSegment[]): UrlMatchResult | null => {
    if (segments.length === 1 && uuidPattern.test(segments[0].path)) {
      return { consumed: segments, posParams: { [paramName]: segments[0] } };
    }

    return null;
  };
}

/**
 * Creates a matcher for UUID routes followed by a fixed action suffix such as `reply` or `edit`.
 *
 * @param paramName Name under which the UUID segment is exposed to the component.
 * @param suffix Required second route segment.
 * @returns Angular route matcher for UUID action paths.
 */
function uuidWithSuffixRoute(paramName: string, suffix: string): UrlMatcher {
  return (segments: UrlSegment[]): UrlMatchResult | null => {
    if (segments.length === 2 && uuidPattern.test(segments[0].path) && segments[1].path === suffix) {
      return { consumed: segments, posParams: { [paramName]: segments[0] } };
    }

    return null;
  };
}

/**
 * Creates a matcher for prefixed UUID routes such as ticket detail URLs.
 *
 * @param prefix Required first route segment.
 * @param paramName Name under which the UUID segment is exposed to the component.
 * @returns Angular route matcher for prefixed UUID paths.
 */
function prefixWithUuidRoute(prefix: string, paramName: string): UrlMatcher {
  return (segments: UrlSegment[]): UrlMatchResult | null => {
    if (segments.length === 2 && segments[0].path === prefix && uuidPattern.test(segments[1].path)) {
      return { consumed: segments, posParams: { [paramName]: segments[1] } };
    }

    return null;
  };
}

/**
 * Application routes for public login, authenticated mail workspace and fallback pages.
 */
export const routes: Routes = [
  { path: 'login', component: LoginPage },
  { path: '', redirectTo: '/mails', pathMatch: 'full' },
  {
    path: 'mails',
    component: RootLayout,
    canActivate: [AuthGuard],
    children: [
      { path: '', component: TicketsPage },
      { path: 'inbox', component: MailInbox },
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
