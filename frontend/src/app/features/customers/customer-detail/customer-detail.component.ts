import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CardModule } from 'primeng/card';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { MessageModule } from 'primeng/message';

import { CustomerService } from '../services/customer.service';
import { Customer, DocumentType } from '../services/customer.models';
import { SaleService } from '../../sales/services/sale.service';
import { SaleStatus, SaleSummary } from '../../sales/services/sales.models';
import { AuthService } from '../../../core/services/auth.service';

const DOCUMENT_TYPE_OPTIONS: { label: string; value: DocumentType }[] = [
  { label: 'Cedula de ciudadania', value: 'CC' },
  { label: 'Cedula de extranjeria', value: 'CE' },
  { label: 'Tarjeta de identidad', value: 'TI' },
  { label: 'Pasaporte', value: 'PASAPORTE' },
  { label: 'NIT', value: 'NIT' }
];

const SALE_STATUS_SEVERITY: Record<SaleStatus, 'success' | 'danger'> = {
  ACTIVA: 'success',
  ANULADA: 'danger'
};

@Component({
  selector: 'app-customer-detail',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, CardModule, TableModule, TagModule, ButtonModule,
    InputTextModule, DropdownModule, MessageModule
  ],
  templateUrl: './customer-detail.component.html'
})
export class CustomerDetailComponent implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly customerService = inject(CustomerService);
  private readonly saleService = inject(SaleService);
  protected readonly authService = inject(AuthService);

  customer = signal<Customer | null>(null);
  saving = signal(false);
  processing = signal(false);
  formError = '';

  documentTypeOptions = DOCUMENT_TYPE_OPTIONS;
  customerForm = this.fb.nonNullable.group({
    documentType: this.fb.control<DocumentType | null>(null),
    documentNumber: [''],
    name: ['', Validators.required],
    phone: [''],
    email: ['', Validators.email],
    address: ['']
  });

  sales: SaleSummary[] = [];
  salesTotalRecords = 0;
  salesLoading = false;

  private customerId!: number;
  private readonly salesPageSize = 10;


  ngOnInit(): void {
    this.customerId = Number(this.route.snapshot.paramMap.get('id'));
    this.reload();
  }

  reload(): void {
    this.customerService.findById(this.customerId).subscribe((customer) => {
      this.customer.set(customer);
      this.customerForm.reset({
        documentType: customer.documentType, documentNumber: customer.documentNumber ?? '',
        name: customer.name, phone: customer.phone ?? '', email: customer.email ?? '', address: customer.address ?? ''
      });
    });
  }

  saleStatusSeverity(status: SaleStatus): 'success' | 'danger' {
    return SALE_STATUS_SEVERITY[status];
  }

  loadSales(event: TableLazyLoadEvent): void {
    this.salesLoading = true;
    const page = Math.floor((event.first ?? 0) / this.salesPageSize);

    this.saleService.search({ customerId: this.customerId, page, size: this.salesPageSize }).subscribe({
      next: (result) => {
        this.sales = result.content;
        this.salesTotalRecords = result.totalElements;
        this.salesLoading = false;
      },
      error: () => (this.salesLoading = false)
    });
  }

  goToSale(sale: SaleSummary): void {
    this.router.navigateByUrl(`/ventas/${sale.id}`);
  }

  submit(): void {
    if (this.customerForm.invalid) {
      this.customerForm.markAllAsTouched();
      return;
    }
    const raw = this.customerForm.getRawValue();
    this.saving.set(true);
    this.formError = '';

    this.customerService.update(this.customerId, {
      documentType: raw.documentType || null,
      documentNumber: raw.documentNumber || null,
      name: raw.name,
      phone: raw.phone || null,
      email: raw.email || null,
      address: raw.address || null
    }).subscribe({
      next: (customer) => {
        this.customer.set(customer);
        this.saving.set(false);
      },
      error: (err) => {
        this.saving.set(false);
        this.formError = err.error?.message || 'No se pudo guardar el cliente.';
      }
    });
  }

  toggleActive(): void {
    const current = this.customer();
    if (!current) {
      return;
    }
    this.processing.set(true);
    this.customerService.setActive(current.id, !current.active).subscribe({
      next: (customer) => {
        this.customer.set(customer);
        this.processing.set(false);
      },
      error: () => this.processing.set(false)
    });
  }

  goBack(): void {
    this.router.navigateByUrl('/clientes');
  }
}
