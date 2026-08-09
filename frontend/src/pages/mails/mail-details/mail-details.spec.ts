import { Location } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { AuthService } from '../../../services/auth/auth-service';
import { MailsService } from '../../../services/mails/mails-service';

import { MailDetails } from './mail-details';

describe('MailDetails', () => {
  let component: MailDetails;
  let fixture: ComponentFixture<MailDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailDetails],
      providers: [
        MessageService,
        { provide: AuthService, useValue: { getCurrentUser: () => null } },
        { provide: MailsService, useValue: { getMailById: () => of(null) } },
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        { provide: Location, useValue: { back: () => undefined } },
      ],
    })
      .compileComponents();

    fixture = TestBed.createComponent(MailDetails);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
