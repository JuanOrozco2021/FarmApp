import { Component, OnInit, signal, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { DropdownModule } from 'primeng/dropdown';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { MessageModule } from 'primeng/message';

import { SaleService } from '../services/sale.service';
import { PaymentMethod, SaleDetail } from '../services/sales.models';
import { ProductService } from '../../products/services/product.service';
import { Presentation, ProductSummary } from '../../products/services/product.models';
import { CustomerService } from '../../customers/services/customer.service';
import { Customer } from '../../customers/services/customer.models';
import { AuthService } from '../../../core/services/auth.service';
import { SaleTicketComponent } from '../sale-ticket/sale-ticket.component';

interface CartLine {
  presentationId: number;
  productName: string;
  unitCode: string;
  conversionFactor: number;
  quantityPresentation: number;
  unitPrice: number;
  discountPercentage: number;
  requiresPrescription: boolean;
}

interface PaymentLine {
  paymentMethod: PaymentMethod;
  amount: number;
}

const PAYMENT_METHOD_OPTIONS: { label: string; value: PaymentMethod }[] = [
  { label: 'Efectivo', value: 'EFECTIVO' },
  { label: 'Tarjeta', value: 'TARJETA' },
  { label: 'Transferencia', value: 'TRANSFERENCIA' }
];

/**
 * Punto de venta: escanear/buscar producto -> carrito -> pagos -> cobrar.
 * El descuento por linea solo es editable si el usuario tiene
 * SALE_APPLY_DISCOUNT (regla confirmada: CAJERO vende, pero en 0%); el
 * backend vuelve a validar esto de todas formas, esta pantalla solo evita
 * que un cajero intente algo que el servidor va a rechazar.
 */
@Component({
  selector: 'app-pos',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ButtonModule, CardModule, InputTextModule,
    InputNumberModule, DropdownModule, TableModule, TagModule, MessageModule, SaleTicketComponent
  ],
  templateUrl: './pos.component.html'
})
export class PosComponent implements OnInit {

  @ViewChild('barcodeInput') barcodeInputRef?: ElementRef<HTMLInputElement>;

  barcodeCode = '';
  scanError = '';

  productQuery = '';
  productResults: ProductSummary[] = [];
  selectedProductName = '';
  selectedProductRequiresPrescription = false;
  presentations: Presentation[] = [];
  draftPresentationId: number | null = null;
  draftQuantity = 1;

  cart: CartLine[] = [];
  customerName = '';
  customerQuery = '';
  customerResults: Customer[] = [];
  selectedCustomer: Customer | null = null;
  payments: PaymentLine[] = [{ paymentMethod: 'EFECTIVO', amount: 0 }];
  paymentMethodOptions = PAYMENT_METHOD_OPTIONS;

  checkingOut = signal(false);
  checkoutError = '';
  lastSale = signal<SaleDetail | null>(null);

  private searchTimeout?: ReturnType<typeof setTimeout>;
  private customerSearchTimeout?: ReturnType<typeof setTimeout>;

  constructor(
    private readonly saleService: SaleService,
    private readonly productService: ProductService,
    private readonly customerService: CustomerService,
    protected readonly authService: AuthService,
    private readonly router: Router
  ) {
  }

  ngOnInit(): void {
  }

  get canApplyDiscount(): boolean {
    return this.authService.hasPermission('SALE_APPLY_DISCOUNT');
  }

  get hasPrescriptionItems(): boolean {
    return this.cart.some((line) => line.requiresPrescription);
  }

  lineGross(line: CartLine): number {
    return line.quantityPresentation * line.unitPrice;
  }

  lineTotal(line: CartLine): number {
    return this.lineGross(line) * (1 - (line.discountPercentage || 0) / 100);
  }

  get subtotal(): number {
    return this.cart.reduce((sum, line) => sum + this.lineGross(line), 0);
  }

  get discountTotal(): number {
    return this.cart.reduce((sum, line) => sum + (this.lineGross(line) - this.lineTotal(line)), 0);
  }

  get total(): number {
    return this.subtotal - this.discountTotal;
  }

  get totalPaid(): number {
    return this.payments.reduce((sum, p) => sum + (p.amount || 0), 0);
  }

  get balance(): number {
    return this.totalPaid - this.total;
  }

  get canCheckout(): boolean {
    return this.cart.length > 0 && this.balance >= -0.009 && this.payments.every((p) => p.amount > 0);
  }

  addByBarcode(): void {
    const code = this.barcodeCode.trim();
    if (!code) {
      return;
    }
    this.scanError = '';

    this.productService.resolveBarcode(code).subscribe({
      next: (resolution) => {
        this.addToCart({
          presentationId: resolution.presentationId,
          productName: resolution.productName,
          unitCode: resolution.unitCode,
          conversionFactor: resolution.conversionFactor,
          quantityPresentation: 1,
          unitPrice: resolution.salePrice,
          discountPercentage: 0,
          requiresPrescription: resolution.requiresPrescription
        });
        this.barcodeCode = '';
        this.barcodeInputRef?.nativeElement.focus();
      },
      error: () => {
        this.scanError = `No se encontro ningun producto con el codigo "${code}"`;
        this.barcodeCode = '';
      }
    });
  }

  onProductQueryChange(): void {
    clearTimeout(this.searchTimeout);
    if (!this.productQuery || this.productQuery.trim().length < 2) {
      this.productResults = [];
      return;
    }
    this.searchTimeout = setTimeout(() => {
      this.productService.search({ search: this.productQuery, page: 0, size: 8 })
        .subscribe((result) => (this.productResults = result.content));
    }, 300);
  }

  selectProduct(product: ProductSummary): void {
    this.selectedProductName = product.name;
    this.selectedProductRequiresPrescription = product.requiresPrescription;
    this.productResults = [];
    this.productQuery = product.name;
    this.draftPresentationId = null;

    this.productService.findById(product.id).subscribe((detail) => {
      this.presentations = detail.presentations.filter((p) => p.active);
    });
  }

  get selectedPresentation(): Presentation | null {
    return this.presentations.find((p) => p.id === this.draftPresentationId) ?? null;
  }

  addSelectedPresentation(): void {
    const presentation = this.selectedPresentation;
    if (!presentation || this.draftQuantity <= 0) {
      return;
    }
    this.addToCart({
      presentationId: presentation.id,
      productName: this.selectedProductName,
      unitCode: presentation.unitCode,
      conversionFactor: presentation.conversionFactor,
      quantityPresentation: this.draftQuantity,
      unitPrice: presentation.salePrice,
      discountPercentage: 0,
      requiresPrescription: this.selectedProductRequiresPrescription
    });

    this.selectedProductName = '';
    this.selectedProductRequiresPrescription = false;
    this.productQuery = '';
    this.presentations = [];
    this.draftPresentationId = null;
    this.draftQuantity = 1;
  }

  private addToCart(line: CartLine): void {
    const existing = this.cart.find((l) => l.presentationId === line.presentationId);
    if (existing) {
      existing.quantityPresentation += line.quantityPresentation;
    } else {
      this.cart.push(line);
    }
  }

  removeLine(index: number): void {
    this.cart.splice(index, 1);
  }

  onCustomerQueryChange(): void {
    clearTimeout(this.customerSearchTimeout);
    if (!this.customerQuery || this.customerQuery.trim().length < 2) {
      this.customerResults = [];
      return;
    }
    this.customerSearchTimeout = setTimeout(() => {
      this.customerService.search({ search: this.customerQuery, active: true, page: 0, size: 8 })
        .subscribe((result) => (this.customerResults = result.content));
    }, 300);
  }

  selectCustomer(customer: Customer): void {
    this.selectedCustomer = customer;
    this.customerQuery = '';
    this.customerResults = [];
  }

  clearCustomer(): void {
    this.selectedCustomer = null;
  }

  addPayment(): void {
    this.payments.push({ paymentMethod: 'EFECTIVO', amount: 0 });
  }

  removePayment(index: number): void {
    if (this.payments.length > 1) {
      this.payments.splice(index, 1);
    }
  }

  fillRemaining(index: number): void {
    const others = this.payments.filter((_, i) => i !== index).reduce((sum, p) => sum + (p.amount || 0), 0);
    const remaining = this.total - others;
    this.payments[index].amount = remaining > 0 ? Math.round(remaining * 100) / 100 : 0;
  }

  checkout(): void {
    if (!this.canCheckout) {
      return;
    }
    this.checkoutError = '';
    this.checkingOut.set(true);

    this.saleService.create({
      customerId: this.selectedCustomer?.id ?? null,
      customerName: this.selectedCustomer ? null : (this.customerName || null),
      items: this.cart.map((line) => ({
        presentationId: line.presentationId,
        quantityPresentation: line.quantityPresentation,
        discountPercentage: line.discountPercentage || 0
      })),
      payments: this.payments.map((p) => ({ paymentMethod: p.paymentMethod, amount: p.amount }))
    }).subscribe({
      next: (sale) => {
        this.checkingOut.set(false);
        this.lastSale.set(sale);
        this.resetCart();
      },
      error: (err: HttpErrorResponse) => {
        this.checkingOut.set(false);
        this.checkoutError = err.error?.message || 'No se pudo registrar la venta. Intenta nuevamente.';
      }
    });
  }

  newSale(): void {
    this.lastSale.set(null);
  }

  goToHistory(): void {
    this.router.navigateByUrl('/ventas');
  }

  print(): void {
    window.print();
  }

  private resetCart(): void {
    this.cart = [];
    this.customerName = '';
    this.customerQuery = '';
    this.customerResults = [];
    this.selectedCustomer = null;
    this.payments = [{ paymentMethod: 'EFECTIVO', amount: 0 }];
  }
}
