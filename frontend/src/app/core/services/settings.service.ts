import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

export interface BusinessSettings {
  pharmacyName: string;
  nit: string | null;
  address: string | null;
  expirationAlertDays: number;
}

export interface UpdateBusinessSettingsPayload {
  pharmacyName: string;
  nit: string | null;
  address: string | null;
  expirationAlertDays: number;
}

/**
 * Configuracion global del negocio. GET es publico para cualquier usuario
 * autenticado (el ticket de venta lo necesita sin importar el rol de quien
 * cobra); PUT lo protege el backend con SETTINGS_MANAGE.
 */
@Injectable({ providedIn: 'root' })
export class SettingsService {

  constructor(private readonly http: HttpClient) {
  }

  get(): Observable<BusinessSettings> {
    return this.http.get<BusinessSettings>(`${environment.apiUrl}/settings`);
  }

  update(payload: UpdateBusinessSettingsPayload): Observable<BusinessSettings> {
    return this.http.put<BusinessSettings>(`${environment.apiUrl}/settings`, payload);
  }
}
