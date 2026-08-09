import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Root Angular component that hosts routed pages and corrects Docker base-path deep links.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('frontend');

  constructor() {
    this.redirectRootSpaPathToAppBase();
  }

  /**
   * Redirects root-level SPA routes to `/app/` when the app is served by Caddy in Docker.
   */
  private redirectRootSpaPathToAppBase(): void {
    const baseHref = document.querySelector('base')?.getAttribute('href');
    const path = window.location.pathname;
    const rootSpaPath = /^(\/mails(?:\/.*)?|\/login|\/404)$/.test(path);

    if (baseHref === '/app/' && rootSpaPath) {
      window.location.replace(`/app${path}${window.location.search}${window.location.hash}`);
    }
  }
}
