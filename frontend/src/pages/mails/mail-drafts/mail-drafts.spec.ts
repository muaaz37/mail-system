import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MessageService } from 'primeng/api';
import { of } from 'rxjs';

import { MailsService } from '../../../services/mails/mails-service';
import { MailDrafts } from './mail-drafts';

describe('MailDrafts', () => {
  let component: MailDrafts;
  let fixture: ComponentFixture<MailDrafts>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailDrafts],
      providers: [
        MessageService,
        { provide: MailsService, useValue: { getDrafts: () => of([]) } },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(MailDrafts);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
