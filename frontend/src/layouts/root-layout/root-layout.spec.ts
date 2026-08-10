import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from '../../services/auth/auth-service';
import { RootLayout } from './root-layout';

describe('RootLayout', () => {
  let component: RootLayout;
  let fixture: ComponentFixture<RootLayout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RootLayout],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { logout: () => undefined } },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(RootLayout);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
