import { describe, expect, it } from 'vitest';
import { MailDeliveryMode } from '../../../../types/mails';
import {
  areEmailAddressesValid,
  buildReplySubject,
  mapMailFormToCreateMail,
  normalizeReplySubject,
  parseEmailAddresses,
} from './mail-form.mapper';

const internalRecipients = {
  toIds: ['to-id'],
  ccIds: ['cc-id'],
  bccIds: ['bcc-id'],
  replyToIds: ['reply-id'],
};

describe('mail form mapper', () => {
  it('parses and validates recipient input', () => {
    expect(
      parseEmailAddresses('Jane Doe <jane@example.org>; support@example.org'),
    ).toEqual(['jane@example.org', 'support@example.org']);
    expect(areEmailAddressesValid(['jane@example.org'])).toBe(true);
    expect(areEmailAddressesValid(['invalid-address'])).toBe(false);
  });

  it('normalizes support reply subjects', () => {
    expect(normalizeReplySubject('[TICKET-42] Re: Re: Login problem')).toBe(
      'Re: Login problem',
    );
    expect(buildReplySubject('Re: Login problem', 'TICKET-42')).toBe(
      '[TICKET-42] Re: Login problem',
    );
  });

  it('maps internal recipients and discards external input', () => {
    const result = mapMailFormToCreateMail(
      {
        subject: 'Internal mail',
        content: 'Message',
        deliveryMode: MailDeliveryMode.INTERNAL,
        externalTo: 'external@example.org',
        externalCc: null,
        externalBcc: null,
        externalReplyTo: null,
      },
      internalRecipients,
      null,
    );

    expect(result.toIds).toEqual(['to-id']);
    expect(result.externalTo).toEqual([]);
  });

  it('maps external addresses and discards internal recipients', () => {
    const result = mapMailFormToCreateMail(
      {
        subject: 'External mail',
        content: 'Message',
        deliveryMode: MailDeliveryMode.EXTERNAL,
        externalTo: 'Customer <customer@example.org>',
        externalCc: 'copy@example.org',
        externalBcc: '',
        externalReplyTo: '',
      },
      internalRecipients,
      'original-mail-id',
    );

    expect(result.toIds).toEqual([]);
    expect(result.externalTo).toEqual(['customer@example.org']);
    expect(result.externalCc).toEqual(['copy@example.org']);
    expect(result.replyToMailId).toBe('original-mail-id');
  });
});
