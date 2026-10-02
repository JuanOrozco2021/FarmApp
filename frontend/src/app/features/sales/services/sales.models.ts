export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type SaleStatus = 'ACTIVA' | 'ANULADA';
export type PaymentMethod = 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA';

export interface SaleItemLotInfo {
  lotCode: string;
  quantityBase: number;
}

export interface SaleItem {
  id: number;
  presentationId: number;
  productName: string;
  unitCode: string;
  conversionFactor: number;
  quantityPresentation: number;
  quantityBase: number;
  unitPrice: number;
  discountPercentage: number;
  lineTotal: number;
  requiresPrescription: boolean;
  lots: SaleItemLotInfo[];
}

export interface SalePayment {
  paymentMethod: PaymentMethod;
  amount: number;
}

export interface SaleSummary {
  id: number;
  saleCode: string;
  customerId: number | null;
  customerName: string | null;
  total: number;
  status: SaleStatus;
  createdAt: string;
}

export interface SaleDetail {
  id: number;
  saleCode: string;
  customerId: number | null;
  customerName: string | null;
  subtotal: number;
  discountTotal: number;
  total: number;
  changeAmount: number;
  status: SaleStatus;
  createdAt: string;
  cancelledAt: string | null;
  cancelledReason: string | null;
  hasPrescriptionItems: boolean;
  items: SaleItem[];
  payments: SalePayment[];
}

export interface SaleItemPayload {
  presentationId: number;
  quantityPresentation: number;
  discountPercentage?: number | null;
}

export interface SalePaymentPayload {
  paymentMethod: PaymentMethod;
  amount: number;
}

export interface CreateSalePayload {
  customerId?: number | null;
  customerName?: string | null;
  items: SaleItemPayload[];
  payments: SalePaymentPayload[];
}
