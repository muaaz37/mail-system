import { Component } from '@angular/core';
import { MailForm } from '../../../components/mails/mail-form/mail-form';

@Component({
  selector: 'app-mail-create',
  imports: [MailForm],
  templateUrl: './mail-create.html',
})
export class MailCreate {}
