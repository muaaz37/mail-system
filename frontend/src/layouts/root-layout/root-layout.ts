import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SideNavigation } from '../../components/common/side-navigation/side-navigation';

/**
 * Hosts the authenticated mail workspace with persistent navigation and routed page content.
 */
@Component({
  selector: 'app-home-page',
  imports: [SideNavigation, RouterOutlet],
  templateUrl: './root-layout.html',
  styleUrl: './root-layout.css',
})
export class RootLayout {}
