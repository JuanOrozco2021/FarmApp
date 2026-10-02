export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Supplier {
  id: number;
  name: string;
  taxId: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  contactName: string | null;
  paymentTermsDays: number | null;
  notes: string | null;
  active: boolean;
}

export interface SupplierPayload {
  name: string;
  taxId?: string | null;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  contactName?: string | null;
  paymentTermsDays?: number | null;
  notes?: string | null;
}

export type PurchaseStatus = 'BORRADOR' | 'RECIBIDA' | 'ANULADA';

export interface PurchaseItemPayload {
  presentationId: number;
  quantityPresentation: number;
  unitCost: number;
  lotCode?: string | null;
  expirationDate?: string | null;
}

export interface CreatePurchasePayload {
  supplierId: number;
  invoiceNumber?: string | null;
  purchaseDate?: string | null;
  notes?: string | null;
  items: PurchaseItemPayload[];
}

export interface PurchaseItem {
  id: number;
  presentationId: number;
  productName: string;
  unitCode: string;
  conversionFactor: number;
  quantityPresentation: number;
  quantityBase: number;
  unitCost: number;
  lineTotal: number;
  lotCode: string | null;
  expirationDate: string | null;
  movementId: number | null;
  returnedQuantityBase: number;
}

export interface PurchaseSummary {
  id: number;
  purchaseCode: string;
  supplierId: number;
  supplierName: string;
  invoiceNumber: string | null;
  purchaseDate: string;
  status: PurchaseStatus;
  totalAmount: number;
}

export interface PurchaseDetail {
  id: number;
  purchaseCode: string;
  supplierId: number;
  supplierName: string;
  invoiceNumber: string | null;
  purchaseDate: string;
  status: PurchaseStatus;
  totalAmount: number;
  notes: string | null;
  receivedAt: string | null;
  items: PurchaseItem[];
}

export interface ReturnItemPayload {
  quantityBase: number;
  reason: string;
}
