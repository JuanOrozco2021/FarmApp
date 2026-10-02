import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { CreateUserPayload, RoleOption, UserSummary } from './user.models';

/**
 * Administracion de OTROS usuarios (crear, activar/desactivar, reasignar
 * roles, resetear contrasena). El backend exige USER_MANAGE en todos estos
 * endpoints; esta pantalla solo se muestra a quien tiene ese permiso, pero
 * el servidor es quien realmente lo hace cumplir.
 */
@Injectable({ providedIn: 'root' })
export class UserService {

  private readonly baseUrl = `${environment.apiUrl}/users`;

  constructor(private readonly http: HttpClient) {
  }

  findAll(): Observable<UserSummary[]> {
    return this.http.get<UserSummary[]>(this.baseUrl);
  }

  create(payload: CreateUserPayload): Observable<UserSummary> {
    return this.http.post<UserSummary>(this.baseUrl, payload);
  }

  setActive(id: number, active: boolean): Observable<UserSummary> {
    return this.http.patch<UserSummary>(`${this.baseUrl}/${id}/active`, null, { params: { active } });
  }

  updateRoles(id: number, roleCodes: string[]): Observable<UserSummary> {
    return this.http.put<UserSummary>(`${this.baseUrl}/${id}/roles`, { roleCodes });
  }

  resetPassword(id: number, newPassword: string): Observable<UserSummary> {
    return this.http.patch<UserSummary>(`${this.baseUrl}/${id}/password`, { newPassword });
  }

  findRoles(): Observable<RoleOption[]> {
    return this.http.get<RoleOption[]>(`${environment.apiUrl}/roles`);
  }
}
