import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { SelectButtonModule } from 'primeng/selectbutton';
import { CalendarModule } from 'primeng/calendar';
import { TagModule } from 'primeng/tag';

import { AuthService } from '../../core/services/auth.service';
import { ReportService } from './services/report.service';
import { InventoryAlertsSummary, SalesSummary, TopProduct } from './services/report.models';
import { InventoryService } from '../inventory/services/inventory.service';
import { InventoryValuation } from '../inventory/services/inventory.models';

type RangeOption = 'HOY' | 'SEMANA' | 'MES' | 'PERSONALIZADO';

const RANGE_OPTIONS: { label: string; value: RangeOption }[] = [
  { label: 'Hoy', value: 'HOY' },
  { label: 'Ultimos 7 dias', value: 'SEMANA' },
  { label: 'Ultimos 30 dias', value: 'MES' },
  { label: 'Personalizado', value: 'PERSONALIZADO' }
];

/**
 * Dashboard principal (Fase 9): resumen de ventas, productos mas vendidos
 * y alertas de inventario. Solo lectura — reutiliza los mismos datos que
 * ya registran Ventas e Inventario, no agrega nada nuevo.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, CardModule, TableModule, SelectButtonModule, CalendarModule, TagModule],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {

  rangeOptions = RANGE_OPTIONS;
  selectedRange: RangeOption = 'HOY';
  customDates: Date[] = [];

  loading = false;
  salesSummary: SalesSummary | null = null;
  topProducts: TopProduct[] = [];
  inventoryAlerts: InventoryAlertsSummary | null = null;
  inventoryValuation: InventoryValuation | null = null;

  constructor(
    protected readonly authService: AuthService,
    private readonly reportService: ReportService,
    private readonly inventoryService: InventoryService,
    private readonly router: Router
  ) {
  }

  ngOnInit(): void {
    this.reload();
    // El valor de inventario es una foto del momento, no depende del
    // rango de fechas seleccionado -- se carga una sola vez.
    this.inventoryService.getValuation().subscribe({
      next: (valuation) => (this.inventoryValuation = valuation),
      error: () => (this.inventoryValuation = null)
    });
  }

  onRangeChange(): void {
    if (this.selectedRange === 'PERSONALIZADO' && this.customDates.length < 2) {
      return;
    }
    this.reload();
  }

  onCustomDatesChange(): void {
    if (this.selectedRange === 'PERSONALIZADO' && this.customDates.length === 2 && this.customDates[1]) {
      this.reload();
    }
  }

  reload(): void {
    const range = this.resolveRange();
    if (!range) {
      return;
    }
    this.loading = true;

    this.reportService.salesSummary(range.from, range.to).subscribe({
      next: (summary) => (this.salesSummary = summary),
      error: () => (this.salesSummary = null)
    });

    this.reportService.topProducts(range.from, range.to, 10).subscribe({
      next: (products) => (this.topProducts = products),
      error: () => (this.topProducts = [])
    });

    this.reportService.inventoryAlertsSummary().subscribe({
      next: (alerts) => {
        this.inventoryAlerts = alerts;
        this.loading = false;
      },
      error: () => {
        this.inventoryAlerts = null;
        this.loading = false;
      }
    });
  }

  goToInventoryAlerts(): void {
    this.router.navigateByUrl('/inventario/alertas');
  }

  private resolveRange(): { from: Date; to: Date } | null {
    const now = new Date();
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0);

    switch (this.selectedRange) {
      case 'HOY':
        return { from: startOfToday, to: now };
      case 'SEMANA': {
        const from = new Date(startOfToday);
        from.setDate(from.getDate() - 6);
        return { from, to: now };
      }
      case 'MES': {
        const from = new Date(startOfToday);
        from.setDate(from.getDate() - 29);
        return { from, to: now };
      }
      case 'PERSONALIZADO': {
        if (this.customDates.length < 2 || !this.customDates[0] || !this.customDates[1]) {
          return null;
        }
        const from = new Date(this.customDates[0]);
        from.setHours(0, 0, 0, 0);
        const to = new Date(this.customDates[1]);
        to.setHours(23, 59, 59, 999);
        return { from, to };
      }
      default:
        return null;
    }
  }
}
