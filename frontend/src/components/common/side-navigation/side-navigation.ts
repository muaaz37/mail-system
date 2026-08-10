import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../services/auth/auth-service';

@Component({
  selector: 'app-side-navigation',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './side-navigation.html',
  styleUrl: './side-navigation.css',
})
export class SideNavigation {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  navigationItems = [
    {
      label: 'Open Tickets',
      description: 'Needs support',
      icon: 'pi pi-inbox',
      route: '/mails',
      exact: true,
    },
    {
      label: 'Internal Inbox',
      description: 'Team messages',
      icon: 'pi pi-envelope',
      route: '/mails/inbox',
      exact: true,
    },
    {
      label: 'Waiting',
      description: 'External reply pending',
      icon: 'pi pi-clock',
      route: '/mails/waiting',
      exact: true,
    },
    {
      label: 'Resolved',
      description: 'Closed tickets',
      icon: 'pi pi-check-circle',
      route: '/mails/resolved',
      exact: true,
    },
    {
      label: 'Drafts',
      description: 'Work in progress',
      icon: 'pi pi-file-edit',
      route: '/mails/drafts',
      exact: true,
    },
    {
      label: 'Sent',
      description: 'Sent mails',
      icon: 'pi pi-send',
      route: '/mails/sent',
      exact: true,
    },
  ];

  /**
   * Opens the compose page for creating a new mail.
   */
  createMail(): void {
    this.router.navigate(['/mails/create']);
  }

  /**
   * Ends the current identity-provider session.
   */
  logout(): void {
    this.authService.logout();
  }
}
