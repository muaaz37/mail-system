import { CommonModule } from '@angular/common';
import { Component, input, model } from '@angular/core';
import { FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { MailDeliveryMode } from '../../../../types/mails';
import { User } from '../../../../types/user';

@Component({
  selector: 'app-mail-form-recipients',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, InputTextModule, MultiSelectModule],
  templateUrl: './mail-form-recipients.html',
})
/**
 * Component for managing mail recipients in the form.
 */
export class MailFormRecipients {
  readonly form = input.required<FormGroup>();
  readonly deliveryMode = input.required<MailDeliveryMode>();
  readonly availableUsers = input.required<User[]>();
  readonly reply = input.required<boolean>();
  readonly internalToDisplayValue = input.required<string>();

  readonly selectedToUsers = model.required<string[]>();
  readonly selectedCcUsers = model.required<string[]>();
  readonly selectedBccUsers = model.required<string[]>();
  readonly selectedReplyToUsers = model.required<string[]>();

  protected readonly MailDeliveryMode = MailDeliveryMode;
}
