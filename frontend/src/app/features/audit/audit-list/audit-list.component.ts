import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';

import { AuditService } from '../services/audit.service';
import { AuditLogEntry } from '../services/audit.models';

/**
 * Consulta del historial de auditoria (Fase 10). Requiere AUDIT_VIEW —
 * solo ADMIN lo tiene hoy — asi que la ruta y el enlace de navegacion
 * quedan ocultos para cualquier otro rol (el backend igual lo exige).
 */
@Component({
  selector: 'app-audit-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, InputTextModule, CalendarModule, DialogModule, ButtonModule],
  templateUrl: './audit-list.component.html'
})
export class AuditListComponent {

  private readonly auditService = inject(AuditService);

  entries: AuditLogEntry[] = [];
  totalRecords = 0;
  loading = false;

  entityName = '';
  action = '';
  username = '';
  dateRange: Date[] = [];

  showDetail = false;
  selectedEntry: AuditLogEntry | null = null;

  private page = 0;
  private readonly pageSize = 20;
  private filterTimeout?: ReturnType<typeof setTimeout>;

  load(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.page = Math.floor((event.first ?? 0) / this.pageSize);

    const [from, to] = this.dateRange;

    this.auditService.search({
      entityName: this.entityName || undefined,
      action: this.action || undefined,
      username: this.username || undefined,
      from: from || null,
      to: to || null,
      page: this.page,
      size: this.pageSize
    }).subscribe({
      next: (result) => {
        this.entries = result.content;
        this.totalRecords = result.totalElements;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  onFilterChange(): void {
    clearTimeout(this.filterTimeout);
    this.filterTimeout = setTimeout(() => this.load({ first: 0 }), 300);
  }

  showDetails(entry: AuditLogEntry): void {
    this.selectedEntry = entry;
    this.showDetail = true;
  }

  formatJson(raw: string | null): string {
    if (!raw) {
      return '(sin datos)';
    }
    try {
      return JSON.stringify(JSON.parse(raw), null, 2);
    } catch {
      return raw;
    }
  }
}
