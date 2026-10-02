export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SimpleCatalogEntry {
  id: number;
  name: string;
}

/**
 * Opcion de unidad de medida para selects. A diferencia de
 * SimpleCatalogEntry (categoria/laboratorio/principio activo, donde el
 * backend espera el id numerico), aqui el backend espera `code` (el
 * codigo real, ej. "UNIDAD") en baseUnitCode/unitCode -- `name` es solo
 * la etiqueta combinada "CODIGO — Nombre" para mostrar en el dropdown.
 */
export interface UnitOption {
  id: number;
  code: string;
  name: string;
}

export interface BarcodeInfo {
  id: number;
  code: string;
  primary: boolean;
}

export interface Presentation {
  id: number;
  unitCode: string;
  unitName: string;
  conversionFactor: number;
  salePrice: number;
  purchasePrice: number | null;
  basePresentation: boolean;
  active: boolean;
  barcodes: BarcodeInfo[];
}

export interface ProductSummary {
  id: number;
  internalCode: string;
  name: string;
  categoryName: string | null;
  laboratoryName: string | null;
  baseUnitCode: string;
  basePrice: number | null;
  status: 'ACTIVE' | 'INACTIVE';
  requiresPrescription: boolean;
}

export interface ProductDetail {
  id: number;
  internalCode: string;
  name: string;
  activeIngredientName: string | null;
  concentration: string | null;
  pharmaceuticalForm: string | null;
  categoryName: string | null;
  laboratoryName: string | null;
  brand: string | null;
  description: string | null;
  baseUnitCode: string;
  minStockBase: number;
  maxStockBase: number | null;
  requiresPrescription: boolean;
  fractionable: boolean;
  controlsLot: boolean;
  controlsExpiration: boolean;
  status: 'ACTIVE' | 'INACTIVE';
  presentations: Presentation[];
}

export interface CreateProductPayload {
  name: string;
  activeIngredientId?: number | null;
  concentration?: string | null;
  pharmaceuticalForm?: string | null;
  categoryId?: number | null;
  laboratoryId?: number | null;
  brand?: string | null;
  description?: string | null;
  baseUnitCode: string;
  minStockBase?: number | null;
  maxStockBase?: number | null;
  requiresPrescription: boolean;
  fractionable: boolean;
  controlsLot: boolean;
  controlsExpiration: boolean;
  basePresentation: {
    unitCode: string;
    conversionFactor: number;
    salePrice: number;
    purchasePrice?: number | null;
    barcode?: string | null;
  };
}

export interface AddPresentationPayload {
  unitCode: string;
  conversionFactor: number;
  salePrice: number;
  purchasePrice?: number | null;
  barcode?: string | null;
}

export interface PriceHistoryEntry {
  oldPrice: number;
  newPrice: number;
  changedBy: string | null;
  changedAt: string;
  reason: string | null;
}

export interface BarcodeResolution {
  barcode: string;
  productId: number;
  productName: string;
  presentationId: number;
  unitCode: string;
  conversionFactor: number;
  salePrice: number;
  requiresPrescription: boolean;
  fractionable: boolean;
}
