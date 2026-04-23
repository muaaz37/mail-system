import { Component, inject } from '@angular/core';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {Button} from 'primeng/button';
import {AuthService} from '../../../services/auth/auth-service';

@Component({
  selector: 'app-side-navigation',
  imports: [
    RouterLink,
    RouterLinkActive,
    Button
  ],
  templateUrl: './side-navigation.html',
})

export class SideNavigation {
   private router = inject(Router);
   private authService = inject(AuthService);

   navigationItems = [
    { label: 'Inbox', icon: 'pi pi-inbox', route: '/mails' },
    { label: 'Sent', icon: 'pi pi-send', route: '/mails/sent' },
    { label: 'Drafts', icon: 'pi pi-file', route: '/mails/drafts' },
  ];

  createMail() {
    this.router.navigate(['/mails/create']);
  }

  logout(){
    this.authService.logout()
  }
}
