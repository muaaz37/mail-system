import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MailsList } from './mails-list';

describe('MailsList', () => {
  let component: MailsList;
  let fixture: ComponentFixture<MailsList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailsList]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MailsList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
