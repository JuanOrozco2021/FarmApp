import { Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';

import { SaleService } from '../services/sale.service';
import { SaleDetail, SaleStatus } from '../services/sales.models';
import { AuthService } from '../../../core/services/auth.service';
import { SaleTicketComponent } from '../sale-ticket/sale-ticket.component';

const STATUS_SEVERITY: Record<SaleStatus, 'success' | 'danger'> = {
  ACTIVA: 'success',
  ANULADA: 'danger'
};

@Component({
  selector: 'app-sale-detail',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, CardModule, TableModule, TagModule, ButtonModule,
    DialogModule, InputTextModule, MessageModule, SaleTicketComponent
  ],
  templateUrl: './sale-detail.component.html'
})
export class SaleDetailComponent implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly saleService = inject(SaleService);
  protected readonly authService = inject(AuthService);

  sale = signal<SaleDetail | null>(null);
  processing = signal(false);
  cancelError = '';

  showCancelDialog = false;
  cancelForm = this.fb.nonNullable.group({
    reason: ['', Validators.required]
  });

  private saleId!: number;

  ngOnInit(): void {
    this.saleId = Number(this.route.snapshot.paramMap.get('id'));
    this.reload();
  }

  reload(): void {
    this.saleService.findById(this.saleId).subscribe((sale) => this.sale.set(sale));
  }

  statusSeverity(status: SaleStatus): 'success' | 'danger' {
    return STATUS_SEVERITY[status];
  }

  openCancelDialog(): void {
    this.cancelError = '';
    this.cancelForm.reset({ reason: '' });
    this.showCancelDialog = true;
  }

  submitCancel(): void {
    if (this.cancelForm.invalid) {
      this.cancelForm.markAllAsTouched();
      return;
    }
    this.processing.set(true);
    this.cancelError = '';

    this.saleService.cancel(this.saleId, this.cancelForm.getRawValue().reason).subscribe({
      next: (sale) => {
        this.sale.set(sale);
        this.processing.set(false);
        this.showCancelDialog = false;
      },
      error: (err) => {
        this.processing.set(false);
        this.cancelError = err.error?.message || 'No se pudo anular la venta.';
      }
    });
  }

  goToCustomer(customerId: number): void {
    this.router.navigateByUrl(`/clientes/${customerId}`);
  }

  goBack(): void {
    this.router.navigateByUrl('/ventas');
  }

  @ViewChild('ticket') ticketComponent?: SaleTicketComponent;

  print(): void {
    this.ticketComponent?.imprimir();
  }
}
