import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  AddPresentationPayload,
  BarcodeResolution,
  CreateProductPayload,
  PageResponse,
  Presentation,
  PriceHistoryEntry,
  ProductDetail,
  ProductSummary,
  SimpleCatalogEntry,
  UnitOption
} from './product.models';

export interface ProductSearchParams {
  search?: string;
  categoryId?: number | null;
  laboratoryId?: number | null;
  activeIngredientId?: number | null;
  status?: 'ACTIVE' | 'INACTIVE' | null;
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class ProductService {

  private readonly baseUrl = `${environment.apiUrl}/products`;

  constructor(private readonly http: HttpClient) {
  }

  search(params: ProductSearchParams): Observable<PageResponse<ProductSummary>> {
    let httpParams = new HttpParams().set('page', params.page).set('size', params.size);
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.categoryId) httpParams = httpParams.set('categoryId', params.categoryId);
    if (params.laboratoryId) httpParams = httpParams.set('laboratoryId', params.laboratoryId);
    if (params.activeIngredientId) httpParams = httpParams.set('activeIngredientId', params.activeIngredientId);
    if (params.status) httpParams = httpParams.set('status', params.status);

    return this.http.get<PageResponse<ProductSummary>>(this.baseUrl, { params: httpParams });
  }

  findById(id: number): Observable<ProductDetail> {
    return this.http.get<ProductDetail>(`${this.baseUrl}/${id}`);
  }

  resolveBarcode(code: string): Observable<BarcodeResolution> {
    return this.http.get<BarcodeResolution>(`${this.baseUrl}/barcode/${code}`);
  }

  create(payload: CreateProductPayload): Observable<ProductDetail> {
    return this.http.post<ProductDetail>(this.baseUrl, payload);
  }

  update(id: number, payload: Partial<CreateProductPayload>): Observable<ProductDetail> {
    return this.http.put<ProductDetail>(`${this.baseUrl}/${id}`, payload);
  }

  setStatus(id: number, status: 'ACTIVE' | 'INACTIVE'): Observable<ProductDetail> {
    return this.http.patch<ProductDetail>(`${this.baseUrl}/${id}/status`, null, { params: { status } });
  }

  addPresentation(productId: number, payload: AddPresentationPayload): Observable<Presentation> {
    return this.http.post<Presentation>(`${this.baseUrl}/${productId}/presentations`, payload);
  }

  updatePrice(presentationId: number, newPrice: number, reason?: string): Observable<Presentation> {
    return this.http.patch<Presentation>(`${environment.apiUrl}/presentations/${presentationId}/price`, { newPrice, reason });
  }

  priceHistory(presentationId: number): Observable<PriceHistoryEntry[]> {
    return this.http.get<PriceHistoryEntry[]>(`${environment.apiUrl}/presentations/${presentationId}/price-history`);
  }

  addBarcode(presentationId: number, code: string, primary = false): Observable<unknown> {
    return this.http.post(`${environment.apiUrl}/presentations/${presentationId}/barcodes`, { code, primary });
  }

  deactivateBarcode(barcodeId: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/presentations/barcodes/${barcodeId}`);
  }

  categories(): Observable<SimpleCatalogEntry[]> {
    return this.http.get<SimpleCatalogEntry[]>(`${environment.apiUrl}/categories`);
  }

  laboratories(): Observable<SimpleCatalogEntry[]> {
    return this.http.get<SimpleCatalogEntry[]>(`${environment.apiUrl}/laboratories`);
  }

  activeIngredients(): Observable<SimpleCatalogEntry[]> {
    return this.http.get<SimpleCatalogEntry[]>(`${environment.apiUrl}/active-ingredients`);
  }

  units(): Observable<UnitOption[]> {
    return this.http.get<UnitOption[]>(`${environment.apiUrl}/units`);
  }
}
