import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MailsListElement } from './mails-list-element';

describe('MailsListElement', () => {
  let component: MailsListElement;
  let fixture: ComponentFixture<MailsListElement>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailsListElement]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MailsListElement);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
