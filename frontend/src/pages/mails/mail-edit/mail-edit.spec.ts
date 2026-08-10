import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MessageService } from 'primeng/api';
import { NEVER } from 'rxjs';

import { MailsService } from '../../../services/mails/mails-service';
import { MailEdit } from './mail-edit';

describe('MailEdit', () => {
  let component: MailEdit;
  let fixture: ComponentFixture<MailEdit>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailEdit],
      providers: [
        MessageService,
        { provide: MailsService, useValue: { getMailById: () => NEVER } },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(MailEdit);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
