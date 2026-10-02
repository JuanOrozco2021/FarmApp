import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { InventoryAlertsSummary, SalesSummary, TopProduct } from './report.models';

@Injectable({ providedIn: 'root' })
export class ReportService {

  private readonly baseUrl = `${environment.apiUrl}/reports`;

  constructor(private readonly http: HttpClient) {
  }

  salesSummary(from: Date, to: Date): Observable<SalesSummary> {
    const params = new HttpParams().set('from', from.toISOString()).set('to', to.toISOString());
    return this.http.get<SalesSummary>(`${this.baseUrl}/sales-summary`, { params });
  }

  topProducts(from: Date, to: Date, limit = 10): Observable<TopProduct[]> {
    const params = new HttpParams().set('from', from.toISOString()).set('to', to.toISOString()).set('limit', limit);
    return this.http.get<TopProduct[]>(`${this.baseUrl}/top-products`, { params });
  }

  inventoryAlertsSummary(): Observable<InventoryAlertsSummary> {
    return this.http.get<InventoryAlertsSummary>(`${this.baseUrl}/inventory-alerts-summary`);
  }
}
