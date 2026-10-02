import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { CalendarModule } from 'primeng/calendar';

import { InventoryService } from '../services/inventory.service';
import { AdjustmentType, LotInfo, MovementInfo, StockInfo } from '../services/inventory.models';
import { AuthService } from '../../../core/services/auth.service';

const ADJUSTMENT_TYPES: { label: string; value: AdjustmentType }[] = [
  { label: 'Entrada (positivo)', value: 'POSITIVO' },
  { label: 'Salida (negativo)', value: 'NEGATIVO' },
  { label: 'Merma', value: 'MERMA' },
  { label: 'Vencimiento', value: 'VENCIMIENTO' }
];

/**
 * Vista de inventario de un producto: stock actual, lotes activos, kardex
 * paginado y el formulario de ajuste manual (la unica via para corregir
 * inventario fuera de compras/ventas). Un ajuste positivo puede abrir un
 * lote nuevo o sumar a uno existente por codigo; uno negativo/merma/
 * vencimiento exige elegir uno de los lotes activos listados abajo.
 */
@Component({
  selector: 'app-inventory-kardex',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, TableModule, CardModule, TagModule, ButtonModule,
    DialogModule, DropdownModule, InputTextModule, InputNumberModule, CalendarModule
  ],
  templateUrl: './inventory-kardex.component.html'
})
export class InventoryKardexComponent implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly inventoryService = inject(InventoryService);
  protected readonly authService = inject(AuthService);

  stock = signal<StockInfo | null>(null);
  lots: LotInfo[] = [];
  movements: MovementInfo[] = [];
  totalMovements = 0;
  loadingKardex = false;

  showAdjustmentDialog = false;
  savingAdjustment = signal(false);
  adjustmentTypes = ADJUSTMENT_TYPES;

  adjustmentForm = this.fb.nonNullable.group({
    adjustmentType: this.fb.nonNullable.control<AdjustmentType>('POSITIVO', Validators.required),
    quantityBase: this.fb.nonNullable.control<number>(1, [Validators.required, Validators.min(0.000001)]),
    reason: ['', Validators.required],
    lotId: this.fb.control<number | null>(null),
    lotCode: [''],
    expirationDate: this.fb.control<Date | null>(null)
  });

  private productId!: number;
  private readonly pageSize = 10;

  ngOnInit(): void {
    this.productId = Number(this.route.snapshot.paramMap.get('id'));
    this.reloadSummary();
  }

  get isPositiveAdjustment(): boolean {
    return this.adjustmentForm.controls.adjustmentType.value === 'POSITIVO';
  }

  reloadSummary(): void {
    this.inventoryService.getStock(this.productId).subscribe((stock) => this.stock.set(stock));
    this.inventoryService.getLots(this.productId).subscribe((lots) => (this.lots = lots));
  }

  loadKardex(event: TableLazyLoadEvent): void {
    this.loadingKardex = true;
    const page = Math.floor((event.first ?? 0) / this.pageSize);

    this.inventoryService.getKardex(this.productId, page, this.pageSize).subscribe({
      next: (result) => {
        this.movements = result.content;
        this.totalMovements = result.totalElements;
        this.loadingKardex = false;
      },
      error: () => (this.loadingKardex = false)
    });
  }

  openAdjustmentDialog(): void {
    this.adjustmentForm.reset({
      adjustmentType: 'POSITIVO', quantityBase: 1, reason: '', lotId: null, lotCode: '', expirationDate: null
    });
    this.showAdjustmentDialog = true;
  }

  submitAdjustment(): void {
    if (this.adjustmentForm.invalid) {
      this.adjustmentForm.markAllAsTouched();
      return;
    }
    const raw = this.adjustmentForm.getRawValue();
    this.savingAdjustment.set(true);

    this.inventoryService.createAdjustment({
      productId: this.productId,
      adjustmentType: raw.adjustmentType,
      quantityBase: raw.quantityBase,
      reason: raw.reason,
      lotId: raw.adjustmentType === 'POSITIVO' ? null : raw.lotId,
      lotCode: raw.adjustmentType === 'POSITIVO' ? (raw.lotCode || null) : null,
      expirationDate: raw.adjustmentType === 'POSITIVO' && raw.expirationDate
        ? this.toIsoDate(raw.expirationDate)
        : null
    }).subscribe({
      next: () => {
        this.savingAdjustment.set(false);
        this.showAdjustmentDialog = false;
        this.reloadSummary();
        this.loadKardex({ first: 0 });
      },
      error: () => this.savingAdjustment.set(false)
    });
  }

  goToProduct(): void {
    this.router.navigateByUrl(`/productos/${this.productId}`);
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
