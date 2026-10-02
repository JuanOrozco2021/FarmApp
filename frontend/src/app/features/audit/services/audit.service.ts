import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { AuditLogEntry, PageResponse } from './audit.models';

export interface AuditSearchParams {
  entityName?: string;
  action?: string;
  username?: string;
  from?: Date | null;
  to?: Date | null;
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class AuditService {

  private readonly baseUrl = `${environment.apiUrl}/audit-logs`;

  constructor(private readonly http: HttpClient) {
  }

  search(params: AuditSearchParams): Observable<PageResponse<AuditLogEntry>> {
    let httpParams = new HttpParams().set('page', params.page).set('size', params.size);
    if (params.entityName) httpParams = httpParams.set('entityName', params.entityName);
    if (params.action) httpParams = httpParams.set('action', params.action);
    if (params.username) httpParams = httpParams.set('username', params.username);
    if (params.from) httpParams = httpParams.set('from', params.from.toISOString());
    if (params.to) httpParams = httpParams.set('to', params.to.toISOString());

    return this.http.get<PageResponse<AuditLogEntry>>(this.baseUrl, { params: httpParams });
  }
}
