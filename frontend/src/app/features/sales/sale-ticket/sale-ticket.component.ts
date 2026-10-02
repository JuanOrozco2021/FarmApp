import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SettingsService } from '../../../core/services/settings.service';
import { SaleDetail } from '../services/sales.models';

const PAYMENT_METHOD_LABELS: Record<string, string> = {
  EFECTIVO: 'Efectivo',
  TARJETA: 'Tarjeta',
  TRANSFERENCIA: 'Transferencia'
};

/**
 * Ticket de venta para impresora POS termica (80mm). Se queda oculto en
 * pantalla (ver .print-ticket en styles.scss) y solo se vuelve visible
 * durante window.print() gracias al truco global de @media print que
 * oculta el resto de la pagina -- asi no hace falta ninguna ventana ni
 * dependencia nueva: cualquier impresora POS que ya este instalada como
 * impresora de Windows aparece en el dialogo de impresion del navegador.
 */
@Component({
  selector: 'app-sale-ticket',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sale-ticket.component.html',
  styleUrls: ['./sale-ticket.component.scss']
})
export class SaleTicketComponent implements OnInit {

  @Input({ required: true }) sale!: SaleDetail;

  pharmacyName = 'Farmacia';
  nit: string | null = null;
  address: string | null = null;

  constructor(private readonly settingsService: SettingsService) {
  }

  ngOnInit(): void {
    this.settingsService.get().subscribe({
      next: (settings) => {
        this.pharmacyName = settings.pharmacyName || this.pharmacyName;
        this.nit = settings.nit || null;
        this.address = settings.address || null;
      },
      error: () => { /* si falla, se queda el nombre generico por defecto y sin NIT/direccion */ }
    });
  }

  paymentLabel(method: string): string {
    return PAYMENT_METHOD_LABELS[method] || method;
  }
}
