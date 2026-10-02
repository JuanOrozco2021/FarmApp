import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { TagModule } from 'primeng/tag';

import { InjectionService } from '../services/injection.service';
import { InjectionRecordSummary, InjectionStatus } from '../services/injection.models';
import { AuthService } from '../../../core/services/auth.service';

const STATUS_OPTIONS: { label: string; value: InjectionStatus }[] = [
  { label: 'Activa', value: 'ACTIVA' },
  { label: 'Anulada', value: 'ANULADA' }
];

const STATUS_SEVERITY: Record<InjectionStatus, 'success' | 'danger'> = {
  ACTIVA: 'success',
  ANULADA: 'danger'
};

/** Servicio de inyectologia (Fase 8): el medicamento siempre lo trae el paciente, nunca toca inventario. */
@Component({
  selector: 'app-injection-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, InputTextModule, DropdownModule, TagModule],
  templateUrl: './injection-list.component.html'
})
export class InjectionListComponent implements OnInit {

  records: InjectionRecordSummary[] = [];
  totalRecords = 0;
  loading = false;

  search = '';
  status: InjectionStatus | null = null;
  statusOptions = STATUS_OPTIONS;

  private page = 0;
  private readonly pageSize = 15;
  private searchTimeout?: ReturnType<typeof setTimeout>;

  constructor(
    private readonly injectionService: InjectionService,
    private readonly router: Router,
    protected readonly authService: AuthService
  ) {
  }

  ngOnInit(): void {
  }

  statusSeverity(status: InjectionStatus): 'success' | 'danger' {
    return STATUS_SEVERITY[status];
  }

  load(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.page = Math.floor((event.first ?? 0) / this.pageSize);

    this.injectionService.search({
      search: this.search || undefined,
      status: this.status,
      page: this.page,
      size: this.pageSize
    }).subscribe({
      next: (result) => {
        this.records = result.content;
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
    this.router.navigateByUrl('/inyectologia/nueva');
  }

  goToDetail(record: InjectionRecordSummary): void {
    this.router.navigateByUrl(`/inyectologia/${record.id}`);
  }
}
