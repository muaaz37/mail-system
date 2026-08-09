import { Component } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { CommonModule } from '@angular/common';

/**
 * Displays the fallback page for routes that are not handled by the application router.
 */
@Component({
  selector: 'app-no-found',
  imports: [ButtonModule, CommonModule],
  templateUrl: './no-found.html',
})
export class NoFound {
  /**
   * Returns the user to the previous browser history entry.
   */
  goBack(): void {
    window.history.back();
  }
}
