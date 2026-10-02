import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

/** Autoservicio de la cuenta del usuario autenticado (distinto de UserService, que administra a otros). */
@Injectable({ providedIn: 'root' })
export class AccountService {

  constructor(private readonly http: HttpClient) {
  }

  changePassword(payload: ChangePasswordPayload): Observable<void> {
    return this.http.patch<void>(`${environment.apiUrl}/account/password`, payload);
  }
}
