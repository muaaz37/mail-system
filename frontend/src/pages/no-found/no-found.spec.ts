import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NoFound } from './no-found';

describe('NoFound', () => {
  let component: NoFound;
  let fixture: ComponentFixture<NoFound>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoFound]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NoFound);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
