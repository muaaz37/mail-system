import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';
import { of } from 'rxjs';

import { MailsService } from '../../../services/mails/mails-service';
import { MailInbox } from './mail-inbox';

describe('MailInbox', () => {
  let component: MailInbox;
  let fixture: ComponentFixture<MailInbox>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailInbox],
      providers: [
        provideRouter([]),
        MessageService,
        { provide: MailsService, useValue: { getIncomingMails: () => of([]) } },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(MailInbox);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
