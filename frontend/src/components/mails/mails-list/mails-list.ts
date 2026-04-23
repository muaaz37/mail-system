import {Component, Input} from '@angular/core';
import { Mail } from '../../../types/mails';
import { MailsListElement } from '../mails-list-element/mails-list-element';

@Component({
  selector: 'app-mails-list',
  imports: [MailsListElement],
  providers: [],
  templateUrl: './mails-list.html',
})
export class MailsList {

  @Input () mails: Mail[] = [];
  @Input() isLoading = false;
  @Input() title: string = '';

}
