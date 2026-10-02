export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type DocumentType = 'CC' | 'CE' | 'TI' | 'PASAPORTE' | 'NIT';

export interface Customer {
  id: number;
  documentType: DocumentType | null;
  documentNumber: string | null;
  name: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  active: boolean;
}

export interface CustomerPayload {
  documentType?: DocumentType | null;
  documentNumber?: string | null;
  name: string;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
}
