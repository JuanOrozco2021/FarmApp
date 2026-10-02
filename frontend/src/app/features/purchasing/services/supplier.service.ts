import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse, Supplier, SupplierPayload } from './purchasing.models';

export interface SupplierSearchParams {
  search?: string;
  active?: boolean | null;
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class SupplierService {

  private readonly baseUrl = `${environment.apiUrl}/suppliers`;

  constructor(private readonly http: HttpClient) {
  }

  search(params: SupplierSearchParams): Observable<PageResponse<Supplier>> {
    let httpParams = new HttpParams().set('page', params.page).set('size', params.size);
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.active !== null && params.active !== undefined) httpParams = httpParams.set('active', params.active);

    return this.http.get<PageResponse<Supplier>>(this.baseUrl, { params: httpParams });
  }

  findById(id: number): Observable<Supplier> {
    return this.http.get<Supplier>(`${this.baseUrl}/${id}`);
  }

  create(payload: SupplierPayload): Observable<Supplier> {
    return this.http.post<Supplier>(this.baseUrl, payload);
  }

  update(id: number, payload: SupplierPayload): Observable<Supplier> {
    return this.http.put<Supplier>(`${this.baseUrl}/${id}`, payload);
  }

  setActive(id: number, active: boolean): Observable<Supplier> {
    return this.http.patch<Supplier>(`${this.baseUrl}/${id}/status`, null, { params: { active } });
  }
}
