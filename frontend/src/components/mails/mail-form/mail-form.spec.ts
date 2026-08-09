import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { AuthService } from '../../../services/auth/auth-service';
import { MailsService } from '../../../services/mails/mails-service';

import { MailForm } from './mail-form';

describe('MailForm', () => {
  let component: MailForm;
  let fixture: ComponentFixture<MailForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailForm],
      providers: [
        MessageService,
        {
          provide: AuthService,
          useValue: { getCurrentUser: () => null },
        },
        {
          provide: MailsService,
          useValue: { getAllUsers: () => of([]) },
        },
        {
          provide: Router,
          useValue: { navigateByUrl: () => Promise.resolve(true) },
        },
      ],
    })
      .compileComponents();

    fixture = TestBed.createComponent(MailForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
