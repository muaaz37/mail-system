import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MailCreate } from './mail-create';

describe('MailCreate', () => {
  let component: MailCreate;
  let fixture: ComponentFixture<MailCreate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailCreate]
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
