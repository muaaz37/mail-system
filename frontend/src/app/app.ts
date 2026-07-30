import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('frontend');

  constructor() {
    this.redirectRootSpaPathToAppBase();
  }

  private redirectRootSpaPathToAppBase() {
    const baseHref = document.querySelector('base')?.getAttribute('href');
    const path = window.location.pathname;
    const rootSpaPath = /^(\/mails(?:\/.*)?|\/login|\/register|\/404)$/.test(path);

    if (baseHref === '/app/' && rootSpaPath) {
      window.location.replace(`/app${path}${window.location.search}${window.location.hash}`);
    }
  }
}
