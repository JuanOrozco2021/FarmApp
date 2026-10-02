export interface PaymentBreakdown {
  paymentMethod: string;
  amount: number;
}

export interface SalesSummary {
  totalSales: number;
  totalRevenue: number;
  totalDiscount: number;
  averageTicket: number;
  paymentBreakdown: PaymentBreakdown[];
}

export interface TopProduct {
  productId: number;
  productName: string;
  quantityBase: number;
  revenue: number;
}

export interface InventoryAlertsSummary {
  lowStockCount: number;
  expiringSoonCount: number;
  expiredCount: number;
}
