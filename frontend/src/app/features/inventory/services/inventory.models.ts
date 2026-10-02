export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface StockInfo {
  productId: number;
  internalCode: string;
  productName: string;
  baseUnitCode: string;
  quantityBase: number;
  minStockBase: number;
  maxStockBase: number | null;
  lowStock: boolean;
}

export interface LotInfo {
  id: number;
  productId: number;
  productName: string;
  lotCode: string;
  expirationDate: string | null;
  quantityBase: number;
  active: boolean;
  expired: boolean;
  expiringSoon: boolean;
}

export interface MovementInfo {
  id: number;
  productId: number;
  productName: string;
  lotCode: string | null;
  movementType: string;
  quantityBase: number;
  balanceAfter: number;
  referenceType: string | null;
  referenceId: number | null;
  userId: number | null;
  occurredAt: string;
  notes: string | null;
}

export type AdjustmentType = 'POSITIVO' | 'NEGATIVO' | 'MERMA' | 'VENCIMIENTO';

export interface CreateAdjustmentPayload {
  productId: number;
  adjustmentType: AdjustmentType;
  quantityBase: number;
  reason: string;
  lotId?: number | null;
  lotCode?: string | null;
  expirationDate?: string | null;
}

export interface AdjustmentInfo {
  id: number;
  productId: number;
  adjustmentType: AdjustmentType;
  quantityBase: number;
  reason: string;
  movementId: number;
  createdAt: string;
}
