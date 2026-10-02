import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  InjectionRecordDetail,
  InjectionRecordPayload,
  InjectionRecordSummary,
  InjectionStatus,
  PageResponse
} from './injection.models';

export interface InjectionSearchParams {
  search?: string;
  status?: InjectionStatus | null;
  customerId?: number | null;
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class InjectionService {

  private readonly baseUrl = `${environment.apiUrl}/injections`;

  constructor(private readonly http: HttpClient) {
  }

  search(params: InjectionSearchParams): Observable<PageResponse<InjectionRecordSummary>> {
    let httpParams = new HttpParams().set('page', params.page).set('size', params.size);
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.customerId) httpParams = httpParams.set('customerId', params.customerId);

    return this.http.get<PageResponse<InjectionRecordSummary>>(this.baseUrl, { params: httpParams });
  }

  findById(id: number): Observable<InjectionRecordDetail> {
    return this.http.get<InjectionRecordDetail>(`${this.baseUrl}/${id}`);
  }

  create(payload: InjectionRecordPayload): Observable<InjectionRecordDetail> {
    return this.http.post<InjectionRecordDetail>(this.baseUrl, payload);
  }

  cancel(id: number, reason: string): Observable<InjectionRecordDetail> {
    return this.http.post<InjectionRecordDetail>(`${this.baseUrl}/${id}/cancel`, { reason });
  }
}
