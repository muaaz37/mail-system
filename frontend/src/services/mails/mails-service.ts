import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../constants';
import { CreateMail, Mail, MailReplyTemplate, UpdateMail } from '../../types/mails';
import { User } from '../../types/user';

@Injectable({
  providedIn: 'root',
})
export class MailsService {
  private readonly http = inject(HttpClient);

  /**
   * Loads all incoming mails visible to the authenticated support user.
   *
   * @returns An observable containing received internal mails and imported support mails.
   */
  getIncomingMails(): Observable<Mail[]> {
    return this.http.get<Mail[]>(`${API_BASE_URL}/mails/incoming`);
  }

  /**
   * Loads drafts created by the authenticated support user.
   *
   * @returns An observable containing editable draft mails.
   */
  getDrafts(): Observable<Mail[]> {
    return this.http.get<Mail[]>(`${API_BASE_URL}/mails/drafts`);
  }

  /**
   * Loads mails sent by the authenticated support user.
   *
   * @returns An observable containing sent mails.
   */
  getSentMails(): Observable<Mail[]> {
    return this.http.get<Mail[]>(`${API_BASE_URL}/mails/sent`);
  }

  /**
   * Loads one mail including recipient, ticket and attachment metadata.
   *
   * @param id Mail identifier returned by the backend.
   * @returns An observable containing the requested mail.
   */
  getMailById(id: string): Observable<Mail> {
    return this.http.get<Mail>(`${API_BASE_URL}/mails/${id}`);
  }

  /**
   * Loads the complete internal conversation containing the selected mail.
   *
   * @param id Mail identifier returned by the backend.
   * @returns An observable containing the conversation ordered by message time.
   */
  getInternalConversation(id: string): Observable<Mail[]> {
    return this.http.get<Mail[]>(`${API_BASE_URL}/mails/${id}/conversation`);
  }

  /**
   * Loads the server-generated reply template for an internal or external mail.
   *
   * @param id Identifier of the mail that should be answered.
   * @returns An observable containing the locked delivery mode, recipients and subject.
   */
  getReplyTemplate(id: string): Observable<MailReplyTemplate> {
    return this.http.get<MailReplyTemplate>(`${API_BASE_URL}/mails/${id}/reply-template`);
  }

  /**
   * Sends an existing draft mail without changing its content first.
   *
   * @param id Draft mail identifier.
   * @returns An observable that completes when sending succeeds.
   */
  sendMail(id: string): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/mails/send/${id}`, {});
  }

  /**
   * Deletes a draft mail and its attachment metadata.
   *
   * @param id Mail identifier.
   * @returns An observable that completes when deletion succeeds.
   */
  deleteMail(id: string): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/mails/${id}`);
  }

  /**
   * Loads team profiles for internal mail recipient selection.
   *
   * @returns An observable containing all known application users.
   */
  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${API_BASE_URL}/users`);
  }

  /**
   * Creates a draft mail with optional uploaded attachments.
   *
   * @param mail Draft payload with internal or external recipients.
   * @param files Browser file objects selected by the user.
   * @returns An observable containing the persisted draft.
   */
  createDraft(mail: CreateMail, files: File[]): Observable<Mail> {
    const formData = this.createFormData(mail, files);
    return this.http.post<Mail>(`${API_BASE_URL}/mails`, formData);
  }

  /**
   * Creates a new mail and immediately sends it through the backend workflow.
   *
   * @param mail Mail payload with internal or external recipients.
   * @param files Browser file objects selected by the user.
   * @returns An observable containing the created sent mail.
   */
  createAndSendMail(mail: CreateMail, files: File[]): Observable<Mail> {
    const formData = this.createFormData(mail, files);
    return this.http.post<Mail>(`${API_BASE_URL}/mails/send`, formData);
  }

  /**
   * Updates an existing draft mail and replaces its attachment set.
   *
   * @param id Draft mail identifier.
   * @param mail Updated draft payload.
   * @param files Current attachment files that should remain on the draft.
   * @returns An observable containing the updated draft.
   */
  updateMail(id: string, mail: UpdateMail, files: File[]): Observable<Mail> {
    const formData = this.createFormData(mail, files);
    return this.http.put<Mail>(`${API_BASE_URL}/mails/${id}`, formData);
  }

  /**
   * Downloads one attachment binary through the backend storage endpoint.
   *
   * @param filename Storage key or attachment path returned by the backend.
   * @returns An observable containing the binary attachment blob.
   */
  fetchAttachment(filename: string): Observable<Blob> {
    return this.http.get(`${API_BASE_URL}/images/${filename}`, { responseType: 'blob' });
  }

  /**
   * Builds the multipart request expected by the backend mail endpoints.
   *
   * @param mail JSON mail payload to send as the `data` part.
   * @param files Attachment files to send as repeated `attachments` parts.
   * @returns Multipart form data containing the JSON payload and attachment parts.
   */
  private createFormData(mail: CreateMail | UpdateMail, files: File[]): FormData {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(mail)], { type: 'application/json' }));

    if (files.length === 0) {
      // Keep the multipart shape stable because the backend endpoint expects the attachments part.
      formData.append('attachments', new Blob([], { type: 'application/octet-stream' }));
    } else {
      files.forEach((file) => {
        formData.append('attachments', file);
      });
    }

    return formData;
  }
}
