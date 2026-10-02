import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageModule } from 'primeng/message';

import { PurchaseService } from '../services/purchase.service';
import { PurchaseDetail, PurchaseItem, PurchaseStatus } from '../services/purchasing.models';
import { AuthService } from '../../../core/services/auth.service';

const STATUS_SEVERITY: Record<PurchaseStatus, 'success' | 'warn' | 'danger'> = {
  BORRADOR: 'warn',
  RECIBIDA: 'success',
  ANULADA: 'danger'
};

@Component({
  selector: 'app-purchase-detail',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, CardModule, TableModule, TagModule, ButtonModule,
    DialogModule, InputTextModule, InputNumberModule, MessageModule
  ],
  templateUrl: './purchase-detail.component.html'
})
export class PurchaseDetailComponent implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly purchaseService = inject(PurchaseService);
  protected readonly authService = inject(AuthService);

  purchase = signal<PurchaseDetail | null>(null);
  processing = signal(false);
  errorMessage = signal<string | null>(null);

  showReturnDialog = false;
  savingReturn = signal(false);
  returnErrorMessage = signal<string | null>(null);
  selectedItem: PurchaseItem | null = null;
  returnForm = this.fb.nonNullable.group({
    quantityBase: this.fb.nonNullable.control<number>(1, [Validators.required, Validators.min(0.000001)]),
    reason: ['', Validators.required]
  });

  private purchaseId!: number;

  ngOnInit(): void {
    this.purchaseId = Number(this.route.snapshot.paramMap.get('id'));
    this.reload();
  }

  reload(): void {
    this.purchaseService.findById(this.purchaseId).subscribe((purchase) => this.purchase.set(purchase));
  }

  statusSeverity(status: PurchaseStatus): 'success' | 'warn' | 'danger' {
    return STATUS_SEVERITY[status];
  }

  availableToReturn(item: PurchaseItem): number {
    return item.quantityBase - item.returnedQuantityBase;
  }

  edit(): void {
    this.router.navigateByUrl(`/compras/${this.purchaseId}/editar`);
  }

  receive(): void {
    this.processing.set(true);
    this.errorMessage.set(null);
    this.purchaseService.receive(this.purchaseId).subscribe({
      next: (purchase) => {
        this.purchase.set(purchase);
        this.processing.set(false);
      },
      error: (err) => {
        this.processing.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo recibir la compra.');
      }
    });
  }

  cancel(): void {
    this.processing.set(true);
    this.errorMessage.set(null);
    this.purchaseService.cancel(this.purchaseId).subscribe({
      next: (purchase) => {
        this.purchase.set(purchase);
        this.processing.set(false);
      },
      error: (err) => {
        this.processing.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo anular la compra.');
      }
    });
  }

  openReturnDialog(item: PurchaseItem): void {
    this.selectedItem = item;
    this.returnForm.reset({ quantityBase: 1, reason: '' });
    this.returnErrorMessage.set(null);
    this.showReturnDialog = true;
  }

  submitReturn(): void {
    if (!this.selectedItem || this.returnForm.invalid) {
      this.returnForm.markAllAsTouched();
      return;
    }
    const raw = this.returnForm.getRawValue();
    this.savingReturn.set(true);

    this.returnErrorMessage.set(null);
    this.purchaseService.returnItem(this.purchaseId, this.selectedItem.id, {
      quantityBase: raw.quantityBase,
      reason: raw.reason
    }).subscribe({
      next: (purchase) => {
        this.purchase.set(purchase);
        this.savingReturn.set(false);
        this.showReturnDialog = false;
      },
      error: (err) => {
        this.savingReturn.set(false);
        this.returnErrorMessage.set(err?.error?.message ?? 'No se pudo registrar la devolucion.');
      }
    });
  }

  goBack(): void {
    this.router.navigateByUrl('/compras');
  }
}
