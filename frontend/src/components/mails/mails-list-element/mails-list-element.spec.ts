import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Mail, MailDeliveryMode, MailSource, MailStatus } from '../../../types/mails';
import { MailsListElement } from './mails-list-element';

describe('MailsListElement', () => {
  let component: MailsListElement;
  let fixture: ComponentFixture<MailsListElement>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailsListElement],
      providers: [provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(MailsListElement);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('mail', createMail());
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

function createMail(): Mail {
  return {
    id: 'mail-1',
    replyToMailId: null,
    sender: null,
    externalSenderEmail: 'customer@example.org',
    externalSenderName: 'Customer',
    externalMessageId: null,
    externalSentAt: null,
    ticketNumber: 'TICKET-123456',
    ticketId: null,
    ticketStatus: null,
    ticketPriority: null,
    ticketAssignedTo: null,
    subject: 'Support request',
    content: 'Please help.',
    status: MailStatus.RECEIVED,
    source: MailSource.EXTERN,
    deliveryMode: MailDeliveryMode.EXTERNAL,
    isRead: false,
    to: [],
    cc: [],
    bcc: [],
    replyTo: [],
    externalTo: [],
    externalCc: [],
    externalBcc: [],
    externalReplyTo: [],
    attachments: [],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    sentAt: undefined,
  };
}
