import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';
import { of } from 'rxjs';

import { AuthService } from '../../../services/auth/auth-service';
import { MailsService } from '../../../services/mails/mails-service';
import { MailCreate } from './mail-create';

describe('MailCreate', () => {
  let component: MailCreate;
  let fixture: ComponentFixture<MailCreate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailCreate],
      providers: [
        provideRouter([]),
        MessageService,
        { provide: AuthService, useValue: { getCurrentUser: () => null } },
        {
          provide: MailsService,
          useValue: {
            getAllUsers: () => of([]),
            getReplyTemplate: () => of(null),
          },
        },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(MailCreate);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
