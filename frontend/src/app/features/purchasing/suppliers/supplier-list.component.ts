import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';

import { SupplierService } from '../services/supplier.service';
import { Supplier } from '../services/purchasing.models';
import { AuthService } from '../../../core/services/auth.service';

/** Dato maestro minimo de proveedor (nombre + contacto). Se amplia en la Fase 7. */
@Component({
  selector: 'app-supplier-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, TableModule, ButtonModule, InputTextModule, TagModule, DialogModule, InputNumberModule],
  templateUrl: './supplier-list.component.html'
})
export class SupplierListComponent implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly supplierService = inject(SupplierService);
  protected readonly authService = inject(AuthService);

  suppliers: Supplier[] = [];
  totalRecords = 0;
  loading = false;
  search = '';

  showDialog = false;
  saving = signal(false);
  editingId: number | null = null;
  supplierForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    taxId: [''],
    phone: [''],
    email: ['', Validators.email],
    address: [''],
    contactName: [''],
    paymentTermsDays: this.fb.control<number | null>(null),
    notes: ['']
  });

  private page = 0;
  private readonly pageSize = 15;
  private searchTimeout?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
  }

  load(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.page = Math.floor((event.first ?? 0) / this.pageSize);

    this.supplierService.search({ search: this.search || undefined, active: null, page: this.page, size: this.pageSize })
      .subscribe({
        next: (result) => {
          this.suppliers = result.content;
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
    this.editingId = null;
    this.supplierForm.reset({
      name: '', taxId: '', phone: '', email: '', address: '',
      contactName: '', paymentTermsDays: null, notes: ''
    });
    this.showDialog = true;
  }

  openEdit(supplier: Supplier): void {
    this.editingId = supplier.id;
    this.supplierForm.reset({
      name: supplier.name, taxId: supplier.taxId ?? '', phone: supplier.phone ?? '',
      email: supplier.email ?? '', address: supplier.address ?? '',
      contactName: supplier.contactName ?? '', paymentTermsDays: supplier.paymentTermsDays,
      notes: supplier.notes ?? ''
    });
    this.showDialog = true;
  }

  submit(): void {
    if (this.supplierForm.invalid) {
      this.supplierForm.markAllAsTouched();
      return;
    }
    const raw = this.supplierForm.getRawValue();
    const payload = {
      name: raw.name,
      taxId: raw.taxId || null,
      phone: raw.phone || null,
      email: raw.email || null,
      address: raw.address || null,
      contactName: raw.contactName || null,
      paymentTermsDays: raw.paymentTermsDays,
      notes: raw.notes || null
    };
    this.saving.set(true);

    const request$ = this.editingId
      ? this.supplierService.update(this.editingId, payload)
      : this.supplierService.create(payload);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.showDialog = false;
        this.load({ first: this.page * this.pageSize });
      },
      error: () => this.saving.set(false)
    });
  }

  toggleActive(supplier: Supplier): void {
    this.supplierService.setActive(supplier.id, !supplier.active)
      .subscribe(() => this.load({ first: this.page * this.pageSize }));
  }
}
