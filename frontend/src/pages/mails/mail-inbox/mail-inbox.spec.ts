import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MailInbox } from './mail-inbox';

describe('MailInbox', () => {
  let component: MailInbox;
  let fixture: ComponentFixture<MailInbox>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailInbox]
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
