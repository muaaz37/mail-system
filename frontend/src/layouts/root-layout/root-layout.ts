import { Component } from '@angular/core';
import {SideNavigation} from '../../components/common/side-navigation/side-navigation';
import {RouterOutlet} from '@angular/router';

@Component({
  selector: 'app-home-page',
  imports: [ SideNavigation, RouterOutlet],
  templateUrl: './root-layout.html',
})
export class RootLayout {}
