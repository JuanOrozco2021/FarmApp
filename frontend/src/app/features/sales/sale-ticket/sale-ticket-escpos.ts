import { EscPosBuilder } from '../../../core/services/escpos-builder';
import { SaleDetail } from '../services/sales.models';

export interface DatosEncabezadoTicket {
  pharmacyName: string;
  nit: string | null;
  address: string | null;
}

const PAYMENT_METHOD_LABELS: Record<string, string> = {
  EFECTIVO: 'Efectivo',
  TARJETA: 'Tarjeta',
  TRANSFERENCIA: 'Transferencia'
};

function formatoMoneda(valor: number): string {
  return valor.toLocaleString('es-CO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatoFecha(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/**
 * Construye el ticket de venta como comandos ESC/POS en crudo, en vez del
 * HTML/CSS que usaba window.print(). anchoColumnas=32 es el estandar para
 * impresoras termicas de 58mm con la fuente por defecto; si la impresora
 * imprime mas o menos caracteres por linea, ajustar este valor.
 */
export function construirTicketEscPos(
  sale: SaleDetail,
  encabezado: DatosEncabezadoTicket,
  anchoColumnas: number = 32
): string[] {
  const b = new EscPosBuilder(anchoColumnas);

  b.alinearCentro()
    .negritaOn().dobleAlturaOn()
    .texto(encabezado.pharmacyName)
    .tamanoNormal().negritaOff();

  if (encabezado.nit) {
    b.texto(`NIT: ${encabezado.nit}`);
  }
  if (encabezado.address) {
    b.texto(encabezado.address);
  }

  b.texto('Ticket de venta')
    .texto(sale.saleCode)
    .texto(formatoFecha(sale.createdAt));

  if (sale.customerName) {
    b.alinearIzquierda().texto(`Cliente: ${sale.customerName}`);
  }

  if (sale.status === 'ANULADA') {
    b.alinearCentro().negritaOn().texto('** VENTA ANULADA **').negritaOff();
  }

  b.alinearIzquierda().lineaSeparadora();

  for (const item of sale.items) {
    b.texto(item.productName);
    b.filaDosColumnas(
      `  ${item.quantityPresentation} ${item.unitCode} x ${formatoMoneda(item.unitPrice)}`,
      formatoMoneda(item.lineTotal)
    );
    if (item.discountPercentage) {
      b.texto(`  Descuento ${item.discountPercentage}%`);
    }
  }

  b.lineaSeparadora()
    .filaDosColumnas('Subtotal', formatoMoneda(sale.subtotal));

  if (sale.discountTotal) {
    b.filaDosColumnas('Descuento', `-${formatoMoneda(sale.discountTotal)}`);
  }

  b.negritaOn()
    .filaDosColumnas('TOTAL', formatoMoneda(sale.total))
    .negritaOff()
    .lineaSeparadora();

  for (const payment of sale.payments) {
    b.filaDosColumnas(PAYMENT_METHOD_LABELS[payment.paymentMethod] || payment.paymentMethod, formatoMoneda(payment.amount));
  }
  if (sale.changeAmount) {
    b.filaDosColumnas('Vuelto', formatoMoneda(sale.changeAmount));
  }

  b.lineaSeparadora()
    .alinearCentro()
    .texto('Gracias por su compra');

  if (sale.hasPrescriptionItems) {
    b.texto('Venta con producto(s) bajo receta medica');
  }

  b.cortarPapel();

  return b.build();
}
