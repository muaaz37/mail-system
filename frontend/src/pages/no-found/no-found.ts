import { Component } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-no-found',
  imports: [ButtonModule, CommonModule],
  templateUrl: './no-found.html',
})
export class NoFound {

  goBack(): void {
    window.history.back();
  }
}
