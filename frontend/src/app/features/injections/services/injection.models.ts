export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type InjectionStatus = 'ACTIVA' | 'ANULADA';
export type InjectionPaymentMethod = 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA';

export interface InjectionRecordSummary {
  id: number;
  injectionCode: string;
  patientName: string;
  medicationName: string;
  appliedAt: string;
  fee: number;
  status: InjectionStatus;
}

export interface InjectionRecordDetail {
  id: number;
  injectionCode: string;
  customerId: number | null;
  patientName: string;
  medicationName: string;
  dose: string | null;
  lotCode: string | null;
  applicationSite: string | null;
  relatedSaleId: number | null;
  appliedAt: string;
  observations: string | null;
  fee: number;
  paymentMethod: InjectionPaymentMethod;
  status: InjectionStatus;
  cancelledAt: string | null;
  cancelledReason: string | null;
}

export interface InjectionRecordPayload {
  customerId?: number | null;
  patientName?: string | null;
  medicationName: string;
  dose?: string | null;
  lotCode?: string | null;
  applicationSite?: string | null;
  relatedSaleId?: number | null;
  observations?: string | null;
  fee: number;
  paymentMethod: InjectionPaymentMethod;
}
