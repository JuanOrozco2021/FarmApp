import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TabViewModule } from 'primeng/tabview';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';

import { InventoryService } from '../services/inventory.service';
import { LotInfo, StockInfo } from '../services/inventory.models';

/**
 * Panel de alertas de inventario: productos por debajo del stock minimo,
 * lotes que vencen dentro de la ventana configurada y lotes ya vencidos.
 * Punto de entrada del modulo de inventario desde el menu principal.
 */
@Component({
  selector: 'app-inventory-alerts',
  standalone: true,
  imports: [CommonModule, TableModule, TabViewModule, TagModule, ButtonModule],
  templateUrl: './inventory-alerts.component.html'
})
export class InventoryAlertsComponent implements OnInit {

  lowStock: StockInfo[] = [];
  expiringSoon: LotInfo[] = [];
  expired: LotInfo[] = [];
  loading = true;

  constructor(private readonly inventoryService: InventoryService, private readonly router: Router) {
  }

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading = true;
    this.inventoryService.lowStockAlerts().subscribe((data) => (this.lowStock = data));
    this.inventoryService.expiringSoonLots().subscribe((data) => (this.expiringSoon = data));
    this.inventoryService.expiredLots().subscribe({
      next: (data) => {
        this.expired = data;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  goToProduct(productId: number): void {
    this.router.navigateByUrl(`/inventario/productos/${productId}`);
  }
}
