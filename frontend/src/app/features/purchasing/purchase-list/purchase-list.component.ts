import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';

import { PurchaseService } from '../services/purchase.service';
import { PurchaseStatus, PurchaseSummary } from '../services/purchasing.models';
import { AuthService } from '../../../core/services/auth.service';

const STATUS_OPTIONS: { label: string; value: PurchaseStatus }[] = [
  { label: 'Borrador', value: 'BORRADOR' },
  { label: 'Recibida', value: 'RECIBIDA' },
  { label: 'Anulada', value: 'ANULADA' }
];

const STATUS_SEVERITY: Record<PurchaseStatus, 'success' | 'warn' | 'danger'> = {
  BORRADOR: 'warn',
  RECIBIDA: 'success',
  ANULADA: 'danger'
};

@Component({
  selector: 'app-purchase-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, InputTextModule, DropdownModule, TagModule],
  templateUrl: './purchase-list.component.html'
})
export class PurchaseListComponent implements OnInit {

  purchases: PurchaseSummary[] = [];
  totalRecords = 0;
  loading = false;

  search = '';
  status: PurchaseStatus | null = null;
  statusOptions = STATUS_OPTIONS;

  private page = 0;
  private readonly pageSize = 15;
  private searchTimeout?: ReturnType<typeof setTimeout>;

  constructor(
    private readonly purchaseService: PurchaseService,
    private readonly router: Router,
    protected readonly authService: AuthService
  ) {
  }

  ngOnInit(): void {
  }

  statusSeverity(status: PurchaseStatus): 'success' | 'warn' | 'danger' {
    return STATUS_SEVERITY[status];
  }

  load(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.page = Math.floor((event.first ?? 0) / this.pageSize);

    this.purchaseService.search({
      search: this.search || undefined,
      status: this.status,
      page: this.page,
      size: this.pageSize
    }).subscribe({
      next: (result) => {
        this.purchases = result.content;
        this.totalRecords = result.totalElements;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  onFilterChange(): void {
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => this.load({ first: 0 }), 300);
  }

  goToNew(): void {
    this.router.navigateByUrl('/compras/nueva');
  }

  goToDetail(purchase: PurchaseSummary): void {
    this.router.navigateByUrl(`/compras/${purchase.id}`);
  }
}
