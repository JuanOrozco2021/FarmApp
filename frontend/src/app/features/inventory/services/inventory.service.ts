import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  AdjustmentInfo,
  CreateAdjustmentPayload,
  InventoryValuation,
  LotInfo,
  MovementInfo,
  PageResponse,
  StockInfo
} from './inventory.models';

@Injectable({ providedIn: 'root' })
export class InventoryService {

  private readonly baseUrl = `${environment.apiUrl}/inventory`;

  constructor(private readonly http: HttpClient) {
  }

  getStock(productId: number): Observable<StockInfo> {
    return this.http.get<StockInfo>(`${this.baseUrl}/products/${productId}/stock`);
  }

  getLots(productId: number): Observable<LotInfo[]> {
    return this.http.get<LotInfo[]>(`${this.baseUrl}/products/${productId}/lots`);
  }

  getKardex(productId: number, page: number, size: number): Observable<PageResponse<MovementInfo>> {
    return this.http.get<PageResponse<MovementInfo>>(`${this.baseUrl}/products/${productId}/kardex`, {
      params: { page, size, sort: 'occurredAt,desc' }
    });
  }

  getValuation(): Observable<InventoryValuation> {
    return this.http.get<InventoryValuation>(`${this.baseUrl}/valuation`);
  }

  lowStockAlerts(): Observable<StockInfo[]> {
    return this.http.get<StockInfo[]>(`${this.baseUrl}/alerts/low-stock`);
  }

  expiredLots(): Observable<LotInfo[]> {
    return this.http.get<LotInfo[]>(`${this.baseUrl}/alerts/expired-lots`);
  }

  expiringSoonLots(): Observable<LotInfo[]> {
    return this.http.get<LotInfo[]>(`${this.baseUrl}/alerts/expiring-lots`);
  }

  createAdjustment(payload: CreateAdjustmentPayload): Observable<AdjustmentInfo> {
    return this.http.post<AdjustmentInfo>(`${this.baseUrl}/adjustments`, payload);
  }
}
