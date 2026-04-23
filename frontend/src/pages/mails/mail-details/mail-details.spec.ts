import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MailDetails } from './mail-details';

describe('MailDetails', () => {
  let component: MailDetails;
  let fixture: ComponentFixture<MailDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailDetails]
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
