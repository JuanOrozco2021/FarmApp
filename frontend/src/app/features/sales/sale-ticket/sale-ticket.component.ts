import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SettingsService } from '../../../core/services/settings.service';
import { QzPrintService } from '../../../core/services/qz-print.service';
import { SaleDetail } from '../services/sales.models';
import { construirTicketEscPos } from './sale-ticket-escpos';

const PAYMENT_METHOD_LABELS: Record<string, string> = {
  EFECTIVO: 'Efectivo',
  TARJETA: 'Tarjeta',
  TRANSFERENCIA: 'Transferencia'
};

/**
 * Ticket de venta. Si el negocio configuro un nombre de impresora
 * (Configuracion > Nombre de la impresora de tickets) y QZ Tray esta
 * corriendo en este equipo, imprimir() envia el ticket como comandos
 * ESC/POS en crudo directo a la impresora -- nitido, sin pasar por el
 * motor de impresion/rasterizado del navegador. Si algo falla (QZ Tray no
 * esta instalado, no esta corriendo, no hay impresora configurada, etc.)
 * cae de vuelta automaticamente al metodo clasico window.print() + CSS de
 * impresion, igual que antes.
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
  private ticketPrinterName: string | null = null;

  constructor(
    private readonly settingsService: SettingsService,
    private readonly qzPrintService: QzPrintService
  ) {
  }

  ngOnInit(): void {
    this.settingsService.get().subscribe({
      next: (settings) => {
        this.pharmacyName = settings.pharmacyName || this.pharmacyName;
        this.nit = settings.nit || null;
        this.address = settings.address || null;
        this.ticketPrinterName = settings.ticketPrinterName || null;
      },
      error: () => { /* si falla, se queda el nombre generico por defecto y sin NIT/direccion */ }
    });
  }

  paymentLabel(method: string): string {
    return PAYMENT_METHOD_LABELS[method] || method;
  }

  async imprimir(): Promise<void> {
    if (!this.sale) {
      return;
    }
    if (!this.ticketPrinterName) {
      // No hay impresora configurada todavia: metodo clasico del navegador.
      window.print();
      return;
    }
    try {
      const comandos = construirTicketEscPos(this.sale, {
        pharmacyName: this.pharmacyName,
        nit: this.nit,
        address: this.address
      });
      await this.qzPrintService.imprimirComandos(this.ticketPrinterName, comandos);
    } catch (error) {
      console.error('No se pudo imprimir via QZ Tray, se usa el dialogo del navegador como respaldo.', error);
      window.print();
    }
  }
}
