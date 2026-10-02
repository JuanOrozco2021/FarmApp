import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';

import { SaleService } from '../services/sale.service';
import { SaleStatus, SaleSummary } from '../services/sales.models';
import { AuthService } from '../../../core/services/auth.service';

const STATUS_OPTIONS: { label: string; value: SaleStatus }[] = [
  { label: 'Activa', value: 'ACTIVA' },
  { label: 'Anulada', value: 'ANULADA' }
];

const STATUS_SEVERITY: Record<SaleStatus, 'success' | 'danger'> = {
  ACTIVA: 'success',
  ANULADA: 'danger'
};

@Component({
  selector: 'app-sale-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, InputTextModule, DropdownModule, TagModule],
  templateUrl: './sale-list.component.html'
})
export class SaleListComponent implements OnInit {

  sales: SaleSummary[] = [];
  totalRecords = 0;
  loading = false;

  search = '';
  status: SaleStatus | null = null;
  statusOptions = STATUS_OPTIONS;

  private page = 0;
  private readonly pageSize = 15;
  private searchTimeout?: ReturnType<typeof setTimeout>;

  constructor(
    private readonly saleService: SaleService,
    private readonly router: Router,
    protected readonly authService: AuthService
  ) {
  }

  ngOnInit(): void {
  }

  statusSeverity(status: SaleStatus): 'success' | 'danger' {
    return STATUS_SEVERITY[status];
  }

  load(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.page = Math.floor((event.first ?? 0) / this.pageSize);

    this.saleService.search({
      search: this.search || undefined,
      status: this.status,
      page: this.page,
      size: this.pageSize
    }).subscribe({
      next: (result) => {
        this.sales = result.content;
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
    this.router.navigateByUrl('/pos');
  }

  goToDetail(sale: SaleSummary): void {
    this.router.navigateByUrl(`/ventas/${sale.id}`);
  }
}
