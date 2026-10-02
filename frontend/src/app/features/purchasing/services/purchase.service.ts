import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  CreatePurchasePayload,
  PageResponse,
  PurchaseDetail,
  PurchaseStatus,
  PurchaseSummary,
  ReturnItemPayload
} from './purchasing.models';

export interface PurchaseSearchParams {
  search?: string;
  supplierId?: number | null;
  status?: PurchaseStatus | null;
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class PurchaseService {

  private readonly baseUrl = `${environment.apiUrl}/purchases`;

  constructor(private readonly http: HttpClient) {
  }

  search(params: PurchaseSearchParams): Observable<PageResponse<PurchaseSummary>> {
    let httpParams = new HttpParams().set('page', params.page).set('size', params.size);
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.supplierId) httpParams = httpParams.set('supplierId', params.supplierId);
    if (params.status) httpParams = httpParams.set('status', params.status);

    return this.http.get<PageResponse<PurchaseSummary>>(this.baseUrl, { params: httpParams });
  }

  findById(id: number): Observable<PurchaseDetail> {
    return this.http.get<PurchaseDetail>(`${this.baseUrl}/${id}`);
  }

  create(payload: CreatePurchasePayload): Observable<PurchaseDetail> {
    return this.http.post<PurchaseDetail>(this.baseUrl, payload);
  }

  update(id: number, payload: CreatePurchasePayload): Observable<PurchaseDetail> {
    return this.http.put<PurchaseDetail>(`${this.baseUrl}/${id}`, payload);
  }

  receive(id: number): Observable<PurchaseDetail> {
    return this.http.post<PurchaseDetail>(`${this.baseUrl}/${id}/receive`, {});
  }

  cancel(id: number): Observable<PurchaseDetail> {
    return this.http.post<PurchaseDetail>(`${this.baseUrl}/${id}/cancel`, {});
  }

  returnItem(purchaseId: number, itemId: number, payload: ReturnItemPayload): Observable<PurchaseDetail> {
    return this.http.post<PurchaseDetail>(`${this.baseUrl}/${purchaseId}/items/${itemId}/return`, payload);
  }
}
