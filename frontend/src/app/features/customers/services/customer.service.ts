import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { Customer, CustomerPayload, PageResponse } from './customer.models';

export interface CustomerSearchParams {
  search?: string;
  active?: boolean | null;
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class CustomerService {

  private readonly baseUrl = `${environment.apiUrl}/customers`;

  constructor(private readonly http: HttpClient) {
  }

  search(params: CustomerSearchParams): Observable<PageResponse<Customer>> {
    let httpParams = new HttpParams().set('page', params.page).set('size', params.size);
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.active !== null && params.active !== undefined) httpParams = httpParams.set('active', params.active);

    return this.http.get<PageResponse<Customer>>(this.baseUrl, { params: httpParams });
  }

  findById(id: number): Observable<Customer> {
    return this.http.get<Customer>(`${this.baseUrl}/${id}`);
  }

  create(payload: CustomerPayload): Observable<Customer> {
    return this.http.post<Customer>(this.baseUrl, payload);
  }

  update(id: number, payload: CustomerPayload): Observable<Customer> {
    return this.http.put<Customer>(`${this.baseUrl}/${id}`, payload);
  }

  setActive(id: number, active: boolean): Observable<Customer> {
    return this.http.patch<Customer>(`${this.baseUrl}/${id}/status`, null, { params: { active } });
  }
}
