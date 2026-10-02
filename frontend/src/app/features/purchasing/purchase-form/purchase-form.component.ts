import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { DropdownModule } from 'primeng/dropdown';
import { CalendarModule } from 'primeng/calendar';
import { TableModule } from 'primeng/table';

import { PurchaseService } from '../services/purchase.service';
import { SupplierService } from '../services/supplier.service';
import { Supplier } from '../services/purchasing.models';
import { ProductService } from '../../products/services/product.service';
import { Presentation, ProductSummary } from '../../products/services/product.models';

interface DraftItem {
  presentationId: number;
  productName: string;
  unitCode: string;
  conversionFactor: number;
  quantityPresentation: number;
  unitCost: number;
  lotCode: string | null;
  expirationDate: string | null;
}

/**
 * Alta/edicion de una compra en BORRADOR. Cada item se arma sobre una
 * presentacion concreta de un producto (se compra en cajas, botellas,
 * etc.) — la conversion a unidad base la hace el backend al recibir, esta
 * pantalla solo previsualiza quantityBase para que quien compra vea de
 * inmediato cuanto va a entrar realmente al inventario.
 */
@Component({
  selector: 'app-purchase-form',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ButtonModule, CardModule, InputTextModule,
    InputNumberModule, DropdownModule, CalendarModule, TableModule
  ],
  templateUrl: './purchase-form.component.html'
})
export class PurchaseFormComponent implements OnInit {

  editingId: number | null = null;
  saving = signal(false);

  suppliers: Supplier[] = [];
  supplierId: number | null = null;
  invoiceNumber = '';
  purchaseDate: Date = new Date();
  notes = '';

  items: DraftItem[] = [];

  productQuery = '';
  productResults: ProductSummary[] = [];
  selectedProductId: number | null = null;
  selectedProductName = '';
  presentations: Presentation[] = [];

  draftPresentationId: number | null = null;
  draftQuantity = 1;
  draftUnitCost = 0;
  draftLotCode = '';
  draftExpirationDate: Date | null = null;

  private searchTimeout?: ReturnType<typeof setTimeout>;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly purchaseService: PurchaseService,
    private readonly supplierService: SupplierService,
    private readonly productService: ProductService
  ) {
  }

  ngOnInit(): void {
    this.supplierService.search({ active: true, page: 0, size: 100 })
      .subscribe((result) => (this.suppliers = result.content));

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editingId = Number(idParam);
      this.purchaseService.findById(this.editingId).subscribe((purchase) => {
        if (purchase.status !== 'BORRADOR') {
          this.router.navigateByUrl(`/compras/${purchase.id}`);
          return;
        }
        this.supplierId = purchase.supplierId;
        this.invoiceNumber = purchase.invoiceNumber ?? '';
        this.purchaseDate = new Date(purchase.purchaseDate);
        this.notes = purchase.notes ?? '';
        this.items = purchase.items.map((item) => ({
          presentationId: item.presentationId,
          productName: item.productName,
          unitCode: item.unitCode,
          conversionFactor: item.conversionFactor,
          quantityPresentation: item.quantityPresentation,
          unitCost: item.unitCost,
          lotCode: item.lotCode,
          expirationDate: item.expirationDate
        }));
      });
    }
  }

  onProductQueryChange(): void {
    clearTimeout(this.searchTimeout);
    if (!this.productQuery || this.productQuery.trim().length < 2) {
      this.productResults = [];
      return;
    }
    this.searchTimeout = setTimeout(() => {
      this.productService.search({ search: this.productQuery, page: 0, size: 8 })
        .subscribe((result) => (this.productResults = result.content));
    }, 300);
  }

  selectProduct(product: ProductSummary): void {
    this.selectedProductId = product.id;
    this.selectedProductName = product.name;
    this.productResults = [];
    this.productQuery = product.name;
    this.draftPresentationId = null;

    this.productService.findById(product.id).subscribe((detail) => {
      this.presentations = detail.presentations.filter((p) => p.active);
    });
  }

  get selectedPresentation(): Presentation | null {
    return this.presentations.find((p) => p.id === this.draftPresentationId) ?? null;
  }

  get draftQuantityBase(): number {
    const presentation = this.selectedPresentation;
    return presentation ? this.draftQuantity * presentation.conversionFactor : 0;
  }

  addItem(): void {
    const presentation = this.selectedPresentation;
    if (!presentation || this.draftQuantity <= 0) {
      return;
    }

    this.items.push({
      presentationId: presentation.id,
      productName: this.selectedProductName,
      unitCode: presentation.unitCode,
      conversionFactor: presentation.conversionFactor,
      quantityPresentation: this.draftQuantity,
      unitCost: this.draftUnitCost,
      lotCode: this.draftLotCode || null,
      expirationDate: this.draftExpirationDate ? this.toIsoDate(this.draftExpirationDate) : null
    });

    this.selectedProductId = null;
    this.selectedProductName = '';
    this.productQuery = '';
    this.presentations = [];
    this.draftPresentationId = null;
    this.draftQuantity = 1;
    this.draftUnitCost = 0;
    this.draftLotCode = '';
    this.draftExpirationDate = null;
  }

  removeItem(index: number): void {
    this.items.splice(index, 1);
  }

  lineTotal(item: DraftItem): number {
    return item.quantityPresentation * item.unitCost;
  }

  get total(): number {
    return this.items.reduce((sum, item) => sum + this.lineTotal(item), 0);
  }

  submit(): void {
    if (!this.supplierId || this.items.length === 0) {
      return;
    }
    this.saving.set(true);

    const payload = {
      supplierId: this.supplierId,
      invoiceNumber: this.invoiceNumber || null,
      purchaseDate: this.toIsoDate(this.purchaseDate),
      notes: this.notes || null,
      items: this.items.map((item) => ({
        presentationId: item.presentationId,
        quantityPresentation: item.quantityPresentation,
        unitCost: item.unitCost,
        lotCode: item.lotCode,
        expirationDate: item.expirationDate
      }))
    };

    const request$ = this.editingId
      ? this.purchaseService.update(this.editingId, payload)
      : this.purchaseService.create(payload);

    request$.subscribe({
      next: (purchase) => {
        this.saving.set(false);
        this.router.navigateByUrl(`/compras/${purchase.id}`);
      },
      error: () => this.saving.set(false)
    });
  }

  goBack(): void {
    this.router.navigateByUrl('/compras');
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
