import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MailEdit } from './mail-edit';

describe('MailEdit', () => {
  let component: MailEdit;
  let fixture: ComponentFixture<MailEdit>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailEdit]
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
