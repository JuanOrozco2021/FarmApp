import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  CreateSalePayload,
  PageResponse,
  SaleDetail,
  SaleStatus,
  SaleSummary
} from './sales.models';

export interface SaleSearchParams {
  search?: string;
  status?: SaleStatus | null;
  customerId?: number | null;
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class SaleService {

  private readonly baseUrl = `${environment.apiUrl}/sales`;

  constructor(private readonly http: HttpClient) {
  }

  search(params: SaleSearchParams): Observable<PageResponse<SaleSummary>> {
    let httpParams = new HttpParams().set('page', params.page).set('size', params.size);
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.customerId) httpParams = httpParams.set('customerId', params.customerId);

    return this.http.get<PageResponse<SaleSummary>>(this.baseUrl, { params: httpParams });
  }

  findById(id: number): Observable<SaleDetail> {
    return this.http.get<SaleDetail>(`${this.baseUrl}/${id}`);
  }

  create(payload: CreateSalePayload): Observable<SaleDetail> {
    return this.http.post<SaleDetail>(this.baseUrl, payload);
  }

  cancel(id: number, reason: string): Observable<SaleDetail> {
    return this.http.post<SaleDetail>(`${this.baseUrl}/${id}/cancel`, { reason });
  }
}
