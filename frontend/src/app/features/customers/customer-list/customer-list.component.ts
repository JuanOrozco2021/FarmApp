import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { MessageModule } from 'primeng/message';

import { CustomerService } from '../services/customer.service';
import { Customer, DocumentType } from '../services/customer.models';
import { AuthService } from '../../../core/services/auth.service';

const DOCUMENT_TYPE_OPTIONS: { label: string; value: DocumentType }[] = [
  { label: 'Cedula de ciudadania', value: 'CC' },
  { label: 'Cedula de extranjeria', value: 'CE' },
  { label: 'Tarjeta de identidad', value: 'TI' },
  { label: 'Pasaporte', value: 'PASAPORTE' },
  { label: 'NIT', value: 'NIT' }
];

/** Dato maestro minimo de cliente (Fase 7): nombre + documento + contacto. */
@Component({
  selector: 'app-customer-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule, TableModule, ButtonModule,
    InputTextModule, TagModule, DialogModule, DropdownModule, MessageModule
  ],
  templateUrl: './customer-list.component.html'
})
export class CustomerListComponent implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly customerService = inject(CustomerService);
  private readonly router = inject(Router);
  protected readonly authService = inject(AuthService);

  customers: Customer[] = [];
  totalRecords = 0;
  loading = false;
  search = '';

  documentTypeOptions = DOCUMENT_TYPE_OPTIONS;
  showDialog = false;
  saving = signal(false);
  formError = '';
  customerForm = this.fb.nonNullable.group({
    documentType: this.fb.control<DocumentType | null>(null),
    documentNumber: [''],
    name: ['', Validators.required],
    phone: [''],
    email: ['', Validators.email],
    address: ['']
  });

  private page = 0;
  private readonly pageSize = 15;
  private searchTimeout?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
  }

  load(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.page = Math.floor((event.first ?? 0) / this.pageSize);

    this.customerService.search({ search: this.search || undefined, active: null, page: this.page, size: this.pageSize })
      .subscribe({
        next: (result) => {
          this.customers = result.content;
          this.totalRecords = result.totalElements;
          this.loading = false;
        },
        error: () => (this.loading = false)
      });
  }

  onFilterChange(): void {
    clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => this.load({ first: 0 }), 300);
  }

  openNew(): void {
    this.customerForm.reset({ documentType: null, documentNumber: '', name: '', phone: '', email: '', address: '' });
    this.formError = '';
    this.showDialog = true;
  }

  submit(): void {
    if (this.customerForm.invalid) {
      this.customerForm.markAllAsTouched();
      return;
    }
    const raw = this.customerForm.getRawValue();
    this.saving.set(true);
    this.formError = '';

    this.customerService.create({
      documentType: raw.documentType || null,
      documentNumber: raw.documentNumber || null,
      name: raw.name,
      phone: raw.phone || null,
      email: raw.email || null,
      address: raw.address || null
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.showDialog = false;
        this.load({ first: this.page * this.pageSize });
      },
      error: (err) => {
        this.saving.set(false);
        this.formError = err.error?.message || 'No se pudo guardar el cliente.';
      }
    });
  }

  goToDetail(customer: Customer): void {
    this.router.navigateByUrl(`/clientes/${customer.id}`);
  }
}
